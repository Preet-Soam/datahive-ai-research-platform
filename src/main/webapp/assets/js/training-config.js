(() => {
    const datasetSelect = document.querySelector("#experiment-dataset");
    const targetSelect = document.querySelector("#target-column");
    const featureList = document.querySelector("[data-feature-list]");
    const featureHint = document.querySelector("[data-feature-hint]");
    const catalog = document.querySelector("[data-dataset-catalog]");
    const modelSelect = document.querySelector("#model-type");
    const logisticParameters = document.querySelector("[data-logistic-parameters]");
    const modelHint = document.querySelector("[data-model-hint]");
    const targetHint = document.querySelector("[data-target-hint]");
    const reproducibilityNote = document.querySelector("[data-reproducibility-note]");
    if (!datasetSelect || !targetSelect || !featureList || !catalog) return;

    const numericTypes = new Set(["INTEGER", "DECIMAL"]);
    let currentColumns = [];

    const makeOption = (value, label) => {
        const option = document.createElement("option");
        option.value = value;
        option.textContent = label;
        return option;
    };

    function renderFeatures(targetName, selectedNames) {
        featureList.replaceChildren();
        const eligible = currentColumns.filter(column =>
            numericTypes.has(column.type) && column.name !== targetName);
        if (!eligible.length) {
            featureHint.textContent = "This dataset has no eligible numeric feature columns for the selected target.";
            return;
        }
        featureHint.textContent = "Choose which numeric columns the model should use. These inputs are saved with the experiment.";
        for (const column of eligible) {
            const label = document.createElement("label");
            label.className = "feature-choice";
            const checkbox = document.createElement("input");
            checkbox.type = "checkbox";
            checkbox.name = "featureColumns";
            checkbox.value = column.name;
            checkbox.checked = selectedNames ? selectedNames.has(column.name) : true;
            const copy = document.createElement("span");
            const name = document.createElement("strong");
            name.textContent = column.name;
            const type = document.createElement("small");
            type.textContent = column.type.toLowerCase();
            copy.append(name, type);
            label.append(checkbox, copy);
            featureList.append(label);
        }
    }

    function updateDataset() {
        const source = catalog.querySelector(`[data-training-dataset="${CSS.escape(datasetSelect.value)}"]`);
        targetSelect.replaceChildren(makeOption("", "Choose a target column"));
        featureList.replaceChildren();
        currentColumns = [];
        if (!source) {
            targetSelect.disabled = true;
            featureHint.textContent = "Select a dataset to see its numeric columns.";
            return;
        }
        currentColumns = Array.from(source.querySelectorAll("[data-column-name]"), column => ({
            name: column.dataset.columnName,
            type: column.dataset.columnType
        }));
        targetSelect.disabled = false;
        for (const column of currentColumns) {
            targetSelect.append(makeOption(column.name, `${column.name} · ${column.type.toLowerCase()}`));
        }
        const preferredTarget = currentColumns.find(column => !numericTypes.has(column.type)) || currentColumns.at(-1);
        if (preferredTarget) targetSelect.value = preferredTarget.name;
        renderFeatures(targetSelect.value);
    }

    function updateModel() {
        const tree = modelSelect?.value === "DECISION_TREE";
        if (logisticParameters) logisticParameters.hidden = tree;
        if (modelHint) modelHint.textContent = tree
            ? "A CART decision tree learns readable threshold splits using Gini impurity. It supports two or more target classes and is capped at depth 6 for this local demo."
            : "Logistic regression learns feature weights with gradient descent. Use it when your target has exactly two classes.";
        if (targetHint) targetHint.textContent = tree
            ? "Choose the label column. The decision tree can classify two or more classes. Numeric fields can be selected below as model inputs."
            : "Choose the label column. Logistic regression requires exactly two classes, such as yes/no. Numeric fields can be selected below as model inputs.";
        if (reproducibilityNote) reproducibilityNote.innerHTML = tree
            ? "<strong>Maximum tree depth: 6</strong><small>Depth is capped to keep the model compact and reduce overfitting.</small>"
            : "<strong>Seed 42</strong><small>Repeating the same setup keeps the split deterministic.</small>";
    }

    datasetSelect.addEventListener("change", updateDataset);
    targetSelect.addEventListener("change", () => renderFeatures(targetSelect.value));
    modelSelect?.addEventListener("change", updateModel);
    updateDataset();
    updateModel();
})();
