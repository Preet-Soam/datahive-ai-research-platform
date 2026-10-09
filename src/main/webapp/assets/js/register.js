(() => {
    const form = document.querySelector(".register-form");
    const invite = document.querySelector("[data-admin-invite]");
    const inviteInput = document.querySelector("#admin-invite");
    if (!form || !invite) return;

    const selectedRole = form.dataset.selectedRole;
    if (selectedRole === "ADMIN" || selectedRole === "RESEARCHER") {
        const selected = form.querySelector(`input[name='role'][value='${selectedRole}']`);
        if (selected) selected.checked = true;
    } else {
        form.querySelector("input[name='role'][value='RESEARCHER']").checked = true;
    }

    const syncRole = () => {
        const isAdmin = form.querySelector("input[name='role']:checked")?.value === "ADMIN";
        invite.hidden = !isAdmin;
        if (inviteInput) inviteInput.required = isAdmin && inviteInput.dataset.enabled === "true";
    };
    if (inviteInput) inviteInput.dataset.enabled = String(inviteInput.required);
    form.addEventListener("change", event => {
        if (event.target?.name === "role") syncRole();
    });
    syncRole();
})();
