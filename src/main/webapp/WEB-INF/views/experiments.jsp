                        <label for="experiment-dataset">Project dataset</label>
                        <select id="experiment-dataset" name="datasetId" required>
                            <option value="">Choose a CSV dataset</option>
                            <c:forEach var="dataset" items="${datasets}"><option value="<c:out value='${dataset.id}'/>"><c:out value="${dataset.name}"/>  |  <c:out value="${dataset.projectTitle}"/></option></c:forEach>
                        </select>
                        <label for="target-column">Binary target header</label><input id="target-column" name="targetColumn" required maxlength="190" placeholder="Exact column name, e.g. churned">
                        <p class="field-hint">The target must have exactly two classes. Every other numeric column becomes a feature; open a dataset profile to check exact headers.</p>
                        <div class="training-parameter-grid"><div><label for="training-epochs">Training epochs</label><select id="training-epochs" name="epochs"><option value="60">60  |  quick</option><option value="120" selected>120  |  balanced</option><option value="180">180  |  extended</option></select></div><div><label for="learning-rate">Learning rate</label><select id="learning-rate" name="learningRate"><option value="0.05">0.05  |  cautious</option><option value="0.12" selected>0.12  |  balanced</option><option value="0.2">0.20  |  faster</option></select></div></div>
                        <div class="experiment-method-note"><span>01</span><p><strong>80/20 stratified split</strong><small>Each class is represented in train and holdout data.</small></p></div>
                        <div class="experiment-method-note"><span>02</span><p><strong>120 training epochs</strong><small>Seed 42 makes the split and training reproducible.</small></p></div>
                        <div class="form-actions"><a class="button button-secondary" href="${pageContext.request.contextPath}/datasets">Inspect datasets</a><button class="button button-primary" type="submit" <c:if test="${empty datasets}">disabled</c:if>>Start training <span>-&gt;</span></button></div>
                    </form>
                    <c:if test="${empty datasets}"><p class="experiment-empty-note">Upload a CSV dataset first to enable training.</p></c:if>
                </article>
                <c:if test="${not empty selectedExperiment}">
                    <article class="panel experiment-detail-panel">
                        <div class="panel-heading"><div><p class="eyebrow">RUN DETAIL  |  #<c:out value="${selectedExperiment.runId}"/></p><h2><c:out value="${selectedExperiment.name}"/></h2></div><a class="back-link" href="${pageContext.request.contextPath}/experiments">Close detail</a></div>
                        <div class="experiment-meta"><span><c:out value="${selectedExperiment.projectTitle}"/></span><span><c:out value="${selectedExperiment.datasetName}"/></span><span>Target: <strong><c:out value="${selectedExperiment.targetColumn}"/></strong></span><span><c:out value="${selectedExperiment.epochs}"/> epochs</span><span>Learning rate <c:out value="${selectedExperiment.learningRateLabel}"/></span></div>
                        <div class="run-progress-row"><span class="status-badge status-<c:out value='${selectedExperiment.statusLabel}'/>"><span></span><c:out value="${selectedExperiment.statusLabel}"/></span><strong><c:out value="${selectedExperiment.progress}"/>%</strong></div>
                        <div class="progress-track"><span style="width:<c:out value='${selectedExperiment.progress}'/>%"></span></div>
                        <c:if test="${selectedExperiment.status eq 'FAILED'}">
                            <div class="alert alert-error" role="alert">
                                <strong>Training failed.</strong>
                                <c:forEach var="log" items="${selectedExperiment.logs}">
                                    <c:if test="${log.levelLabel eq 'error'}">
                                        <p><c:out value="${log.message}"/></p>
                                    </c:if>
                                </c:forEach>
                                <p>Correct the issue and start a new run.</p>
                            </div>
                        </c:if>
                        <c:if test="${selectedExperiment.status eq 'COMPLETED'}"><div class="run-metric-grid"><div><strong><c:out value="${selectedExperiment.accuracyPercent}"/></strong><span>Accuracy</span></div><div><strong><c:out value="${selectedExperiment.f1Percent}"/></strong><span>F1 score</span></div><div><strong><c:out value="${selectedExperiment.precisionPercent}"/></strong><span>Precision</span></div><div><strong><c:out value="${selectedExperiment.recallPercent}"/></strong><span>Recall</span></div></div></c:if>
                        <div class="run-log-heading"><h3>Run log</h3><span>Recorded status and progress</span></div>
                        <ol class="run-log-list"><c:forEach var="log" items="${selectedExperiment.logs}"><li class="log-${log.levelLabel}"><time><c:out value="${log.createdAt}"/></time><span><c:out value="${log.message}"/></span></li></c:forEach></ol>
                    </article>
                </c:if>
            </section>
            <section class="panel project-list-panel experiment-list-panel">
                <div class="panel-heading project-list-heading"><div><p class="eyebrow">EXPERIMENT TRACKING</p><h2>Training runs</h2></div><span class="quiet-label"><c:out value="${experiments.size()}"/> saved</span></div>
                <c:choose><c:when test="${empty experiments}"><div class="project-empty"><span class="empty-mark">ML</span><h3>No experiments yet</h3><p>Start a baseline run to create a persistent experiment, status history, metrics, and run log.</p></div></c:when><c:otherwise>
                    <div class="table-scroll"><table class="data-table"><thead><tr><th>EXPERIMENT</th><th>PROJECT / DATASET</th><th>MODEL / TARGET</th><th>STATUS</th><th>ACCURACY</th><th>F1</th><th>DETAIL</th></tr></thead><tbody>
                        <c:forEach var="experiment" items="${experiments}"><tr>
                            <td><div class="project-name-cell"><span class="experiment-table-icon">ML</span><span><strong><c:out value="${experiment.name}"/></strong><small>Run #<c:out value="${experiment.runId}"/>  |  <c:out value="${experiment.createdBy}"/></small></span></div></td>
                            <td><strong class="profile-column-name"><c:out value="${experiment.projectTitle}"/></strong><br><span class="quiet-label"><c:out value="${experiment.datasetName}"/></span></td>
                            <td><strong class="profile-column-name"><c:out value="${experiment.modelName}"/></strong><br><span class="quiet-label">Target: <c:out value="${experiment.targetColumn}"/></span></td>
                            <td><span class="status-badge status-<c:out value='${experiment.statusLabel}'/>"><span></span><c:out value="${experiment.statusLabel}"/></span><c:if test="${experiment.status eq 'RUNNING' or experiment.status eq 'QUEUED'}"><div class="mini-progress"><span style="width:<c:out value='${experiment.progress}'/>%"></span></div></c:if></td>
                            <td><c:out value="${experiment.accuracyPercent}"/></td><td><c:out value="${experiment.f1Percent}"/></td>
                            <td><a class="text-action" href="${pageContext.request.contextPath}/experiments?view=<c:out value='${experiment.id}'/>">Run log</a></td>
                        </tr></c:forEach>
                    </tbody></table></div>
                </c:otherwise></c:choose>
            </section>
            <footer class="page-footer"><span>DataHive Research Platform</span><span>Metrics come from a deterministic held-out split of the selected CSV.</span></footer>
        </div>
    </main>
</div>
<script src="${pageContext.request.contextPath}/assets/js/app.js?v=20261008-photo-menu4" defer></script>
</body>
</html>
