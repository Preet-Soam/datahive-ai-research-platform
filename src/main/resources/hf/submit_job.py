"""Upload one DataHive CSV to a private Hub dataset and submit a Jobs run."""
import argparse
import json
import os
import sys
import time

from huggingface_hub import HfApi, run_job


def remote_program():
    return r'''import json, os, time, urllib.parse, urllib.request
from huggingface_hub import hf_hub_download
import numpy as np
import pandas as pd
from sklearn.impute import SimpleImputer
from sklearn.linear_model import SGDClassifier
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score
from sklearn.model_selection import train_test_split
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler
from sklearn.tree import DecisionTreeClassifier

def callback(fields):
    fields["runId"] = os.environ["DATAHIVE_RUN_ID"]
    fields["token"] = os.environ["DATAHIVE_CALLBACK_TOKEN"]
    request = urllib.request.Request(os.environ["DATAHIVE_CALLBACK_URL"],
        data=urllib.parse.urlencode(fields).encode(),
        headers={"Content-Type": "application/x-www-form-urlencoded"}, method="POST")
    with urllib.request.urlopen(request, timeout=20) as response:
        if response.status != 200: raise RuntimeError("DataHive did not accept the run result")

started = time.monotonic()
try:
    repo = os.environ["DATAHIVE_DATASET_REPO"]
    csv_path = hf_hub_download(repo_id=repo, repo_type="dataset", filename="dataset.csv", token=os.environ["HF_TOKEN"])
    frame = pd.read_csv(csv_path)
    target = os.environ["DATAHIVE_TARGET"]
    features = json.loads(os.environ["DATAHIVE_FEATURES"])
    y = frame[target].astype("string").fillna("").str.strip()
    keep = y.ne("")
    x = frame.loc[keep, features].apply(pd.to_numeric, errors="coerce")
    y = y.loc[keep]
    if len(y) < 10 or y.nunique() < 2: raise ValueError("Training requires at least 10 labeled rows and 2 target classes.")
    model_kind = os.environ["DATAHIVE_MODEL"]
    if model_kind == "LOGISTIC_REGRESSION":
        if y.nunique() != 2: raise ValueError("Logistic regression requires exactly two target classes.")
        estimator = make_pipeline(SimpleImputer(strategy="median"), StandardScaler(), SGDClassifier(loss="log_loss", max_iter=int(os.environ["DATAHIVE_EPOCHS"]), learning_rate="constant", eta0=float(os.environ["DATAHIVE_LEARNING_RATE"]), tol=None, random_state=42))
    elif model_kind == "DECISION_TREE":
        estimator = make_pipeline(SimpleImputer(strategy="median"), DecisionTreeClassifier(max_depth=6, criterion="gini", random_state=42))
    else: raise ValueError("Unsupported DataHive model type.")
    counts = y.value_counts()
    if counts.min() < 2: raise ValueError("Each class needs at least two rows for a stratified holdout.")
    x_train, x_test, y_train, y_test = train_test_split(x, y, test_size=0.2, random_state=42, stratify=y)
    estimator.fit(x_train, y_train)
    predicted = estimator.predict(x_test)
    callback({"status":"COMPLETED", "accuracy":accuracy_score(y_test,predicted),
        "precision":precision_score(y_test,predicted,average="macro",zero_division=0),
        "recall":recall_score(y_test,predicted,average="macro",zero_division=0),
        "f1":f1_score(y_test,predicted,average="macro",zero_division=0),
        "classSummary":", ".join(str(v) for v in sorted(y.unique().tolist()))[:500],
        "trainRows":len(y_train), "testRows":len(y_test), "elapsedSeconds":time.monotonic()-started})
except Exception as exc:
    try: callback({"status":"FAILED", "message":str(exc)[:1500]})
    except Exception: pass
    raise
'''


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--csv", required=True)
    parser.add_argument("--run-id", required=True)
    parser.add_argument("--experiment-name", required=True)
    parser.add_argument("--model", required=True)
    parser.add_argument("--target", required=True)
    parser.add_argument("--features", required=True)
    parser.add_argument("--epochs", type=int, required=True)
    parser.add_argument("--learning-rate", type=float, required=True)
    parser.add_argument("--callback-url", required=True)
    args = parser.parse_args()

    token = os.environ["HF_TOKEN"]
    namespace = os.environ["HF_NAMESPACE"]
    repo_id = f"{namespace}/datahive-run-{args.run_id}"
    api = HfApi(token=token)
    api.create_repo(repo_id=repo_id, repo_type="dataset", private=True, exist_ok=True)
    api.upload_file(path_or_fileobj=args.csv, path_in_repo="dataset.csv", repo_id=repo_id,
                    repo_type="dataset", commit_message=f"Add private CSV for DataHive run {args.run_id}")

    environment = {
        "DATAHIVE_DATASET_REPO": repo_id,
        "DATAHIVE_CALLBACK_URL": args.callback_url,
        "DATAHIVE_RUN_ID": args.run_id,
        "DATAHIVE_MODEL": args.model,
        "DATAHIVE_TARGET": args.target,
        "DATAHIVE_FEATURES": args.features,
        "DATAHIVE_EPOCHS": str(args.epochs),
        "DATAHIVE_LEARNING_RATE": str(args.learning_rate),
    }
    secrets = {"HF_TOKEN": token,
               "DATAHIVE_CALLBACK_TOKEN": os.environ["DATAHIVE_CALLBACK_TOKEN"]}
    # Install the remote worker dependencies in the otherwise minimal image.
    remote_worker = "import subprocess; subprocess.check_call(['python','-m','pip','install','--quiet','huggingface_hub','pandas','scikit-learn']); exec(" + repr(remote_program()) + ")"
    command = ["python", "-c", remote_worker]
    job = run_job(image="python:3.12", namespace=namespace, flavor=os.environ.get("HF_JOBS_FLAVOR", "cpu-basic"),
                  command=command, env=environment, secrets=secrets, timeout="2h",
                  name=f"datahive-run-{args.run_id}")
    print(json.dumps({"jobId": job.id, "jobUrl": job.url, "datasetRepo": repo_id}), flush=True)


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        print(json.dumps({"error": str(error)[:1500]}), file=sys.stderr, flush=True)
        raise
