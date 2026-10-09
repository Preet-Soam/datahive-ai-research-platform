(() => {
    const form = document.querySelector("[data-google-auth]");
    if (!form) return;
    const slot = form.querySelector("[data-google-button]");
    const clientId = form.dataset.googleClientId;
    let attempts = 0;
    const retry = () => {
        if (!window.google?.accounts?.id) {
            if (++attempts < 60) window.setTimeout(retry, 120);
            else slot.textContent = "Google sign-in did not load. Refresh and try again.";
            return;
        }
        window.google.accounts.id.initialize({
            client_id: clientId,
            nonce: form.dataset.googleNonce,
            auto_select: false,
            callback: response => {
                const credential = form.querySelector("input[name='credential']");
                if (!response?.credential || !credential) return;
                credential.value = response.credential;
                if (form.dataset.googleMode === "register") {
                    const source = document.querySelector(form.dataset.roleSource);
                    form.querySelector("input[name='role']").value = source?.querySelector("input[name='role']:checked")?.value || "RESEARCHER";
                    form.querySelector("input[name='adminInviteCode']").value = source?.querySelector("input[name='adminInviteCode']")?.value || "";
                }
                form.requestSubmit();
            }
        });
        window.google.accounts.id.renderButton(slot, {
            type: "standard",
            theme: "outline",
            size: "large",
            text: form.dataset.googleMode === "register" ? "signup_with" : "signin_with",
            shape: "rectangular",
            logo_alignment: "left",
            width: Math.min(400, Math.max(260, Math.round(slot.getBoundingClientRect().width)))
        });
    };
    retry();
})();
