document.addEventListener("DOMContentLoaded", () => {
    if (document.body.dataset.runLive === "true") {
        window.setTimeout(() => window.location.reload(), 5000);
    }
    const toggle = document.querySelector("[data-toggle-password]");
    const password = document.querySelector("#password");
    if (toggle && password) {
        toggle.addEventListener("click", () => {
            const reveal = password.type === "password";
            password.type = reveal ? "text" : "password";
            toggle.textContent = reveal ? "Hide" : "Show";
            toggle.setAttribute("aria-label", reveal ? "Hide password" : "Show password");
        });
    }

    const menuToggle = document.querySelector("[data-menu-toggle]");
    const sidebar = document.querySelector("[data-sidebar]");
    if (menuToggle && sidebar) {
        menuToggle.addEventListener("click", () => {
            const open = sidebar.classList.toggle("sidebar-open");
            menuToggle.setAttribute("aria-expanded", String(open));
        });
    }

    const accountMenu = document.querySelector("[data-account-menu]");
    if (accountMenu) {
        const accountPanel = accountMenu.querySelector(".account-menu-panel");
        const accountTriggers = document.querySelectorAll("[data-account-menu-toggle]");
        let lastAccountTrigger = null;
        const setAccountMenuOpen = (open, restoreFocus = false) => {
            accountMenu.hidden = !open;
            accountTriggers.forEach((trigger) => trigger.setAttribute("aria-expanded", String(open)));
            if (restoreFocus && lastAccountTrigger) {
                lastAccountTrigger.focus();
            }
        };

        accountTriggers.forEach((trigger) => {
            trigger.addEventListener("click", () => {
                if (typeof closeUtilityLayers === "function") closeUtilityLayers();
                lastAccountTrigger = trigger;
                const shouldOpen = accountMenu.hidden;
                setAccountMenuOpen(shouldOpen);
                if (shouldOpen) {
                    accountMenu.querySelector(".account-menu-close").focus();
                }
            });
        });

        accountMenu.querySelectorAll("[data-account-menu-close]").forEach((button) => {
            button.addEventListener("click", () => setAccountMenuOpen(false, true));
        });
        accountPanel.querySelectorAll("a[href]").forEach((link) => {
            link.addEventListener("click", () => setAccountMenuOpen(false));
        });

        document.addEventListener("keydown", (event) => {
            if (accountMenu.hidden) return;
            if (event.key === "Escape") {
                event.preventDefault();
                setAccountMenuOpen(false, true);
                return;
            }
            if (event.key === "Tab") {
                const focusable = [...accountPanel.querySelectorAll("a[href], button:not([disabled])")];
                const first = focusable[0];
                const last = focusable[focusable.length - 1];
                if (event.shiftKey && document.activeElement === first) {
                    event.preventDefault();
                    last.focus();
                } else if (!event.shiftKey && document.activeElement === last) {
                    event.preventDefault();
                    first.focus();
                }
            }
        });
    }

    const profilePhotoKey = accountMenu?.dataset.profilePhotoKey;
    const profilePhotoImages = [...document.querySelectorAll("[data-profile-photo]")];
    const profileInitials = [...document.querySelectorAll("[data-profile-initials]")];
    const removePhotoButton = document.querySelector("[data-photo-remove]");
    const removePhotoHelp = document.querySelector("[data-photo-remove-help]");
    let savedProfilePhoto = "";
    const applyProfilePhoto = (value) => {
        const photo = typeof value === "string" && value.startsWith("data:image/jpeg;base64,") && value.length < 250000
            ? value
            : "";
        savedProfilePhoto = photo;
        profilePhotoImages.forEach((image) => {
            image.hidden = !photo;
            if (photo) image.src = photo;
            else image.removeAttribute("src");
        });
        profileInitials.forEach((element) => { element.hidden = Boolean(photo); });
        if (removePhotoButton) removePhotoButton.disabled = !photo;
        if (removePhotoHelp) removePhotoHelp.hidden = Boolean(photo);
    };
    if (profilePhotoKey) {
        try { applyProfilePhoto(window.localStorage.getItem(profilePhotoKey)); }
        catch (_) { applyProfilePhoto(""); }
        window.addEventListener("storage", (event) => {
            if (event.key === profilePhotoKey) applyProfilePhoto(event.newValue);
        });
    }

    const photoControl = document.querySelector("[data-photo-control]");
    const photoTrigger = photoControl?.querySelector("[data-photo-actions-toggle]");
    const photoMenu = photoControl?.querySelector("[data-photo-menu]");
    const uploadPhotoButton = photoControl?.querySelector("[data-photo-upload]");
    const fileInput = photoControl?.querySelector("[data-profile-photo-file]");
    const photoStatus = document.querySelector("[data-profile-photo-status]");
    if (photoControl && photoTrigger && photoMenu) {
        let photoMenuPinned = false;
        const openPhotoMenu = () => {
            photoMenu.hidden = false;
            photoTrigger.setAttribute("aria-expanded", "true");
        };
        const closePhotoMenu = (restoreFocus = false) => {
            photoMenu.hidden = true;
            photoMenuPinned = false;
            photoTrigger.setAttribute("aria-expanded", "false");
            if (restoreFocus) photoTrigger.focus();
        };
        photoControl.addEventListener("mouseenter", openPhotoMenu);
        photoControl.addEventListener("mouseleave", () => {
            if (!photoMenuPinned && !photoControl.contains(document.activeElement)) closePhotoMenu();
        });
        photoControl.addEventListener("focusin", openPhotoMenu);
        photoControl.addEventListener("focusout", (event) => {
            if (!photoControl.contains(event.relatedTarget) && !photoMenuPinned) {
                window.setTimeout(() => {
                    if (!photoControl.contains(document.activeElement) && !photoMenuPinned) closePhotoMenu();
                }, 0);
            }
        });
        photoTrigger.addEventListener("click", () => {
            if (photoMenuPinned) closePhotoMenu();
            else {
                photoMenuPinned = true;
                openPhotoMenu();
            }
        });
        document.addEventListener("pointerdown", (event) => {
            if (!photoMenu.hidden && !photoControl.contains(event.target)) closePhotoMenu();
        });
        photoControl.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && !photoMenu.hidden) {
                event.preventDefault();
                closePhotoMenu(true);
            }
        });
        uploadPhotoButton?.addEventListener("click", () => {
            closePhotoMenu();
            fileInput?.click();
        });
        removePhotoButton?.addEventListener("click", () => {
            if (!profilePhotoKey || !savedProfilePhoto) return;
            try {
                window.localStorage.removeItem(profilePhotoKey);
                applyProfilePhoto("");
                if (photoStatus) photoStatus.textContent = "Profile photo removed from this browser.";
                closePhotoMenu();
            } catch (_) {
                if (photoStatus) photoStatus.textContent = "This browser could not remove the saved photo.";
            }
        });
    }

    if (fileInput && profilePhotoKey) {
        fileInput.addEventListener("change", async () => {
            const file = fileInput.files?.[0];
            if (!file) return;
            if (!["image/png", "image/jpeg"].includes(file.type)) {
                if (photoStatus) photoStatus.textContent = "Choose a PNG or JPG image.";
                fileInput.value = "";
                return;
            }
            if (file.size > 5 * 1024 * 1024) {
                if (photoStatus) photoStatus.textContent = "Choose an image smaller than 5 MB.";
                fileInput.value = "";
                return;
            }
            if (uploadPhotoButton) uploadPhotoButton.disabled = true;
            if (removePhotoButton) removePhotoButton.disabled = true;
            if (photoStatus) photoStatus.textContent = "Preparing your picture...";
            let bitmap;
            try {
                bitmap = await createImageBitmap(file);
                if (bitmap.width > 8000 || bitmap.height > 8000 || bitmap.width * bitmap.height > 20000000) {
                    throw new Error("This image is too large to process. Choose a smaller picture.");
                }
                const side = Math.min(bitmap.width, bitmap.height);
                const sourceX = (bitmap.width - side) / 2;
                const sourceY = (bitmap.height - side) / 2;
                const canvas = document.createElement("canvas");
                canvas.width = 256;
                canvas.height = 256;
                const context = canvas.getContext("2d");
                if (!context) throw new Error("This browser could not prepare the picture.");
                context.drawImage(bitmap, sourceX, sourceY, side, side, 0, 0, 256, 256);
                const photo = canvas.toDataURL("image/jpeg", 0.84);
                window.localStorage.setItem(profilePhotoKey, photo);
                applyProfilePhoto(photo);
                if (photoStatus) photoStatus.textContent = "Profile picture saved in this browser.";
            } catch (error) {
                if (photoStatus) photoStatus.textContent = error.message || "The picture could not be saved in this browser.";
            } finally {
                if (bitmap) bitmap.close();
                fileInput.value = "";
                if (uploadPhotoButton) uploadPhotoButton.disabled = false;
                if (removePhotoButton) removePhotoButton.disabled = !savedProfilePhoto;
            }
        });
    }

    const helpLayer = document.querySelector("[data-help]");
    const helpTriggers = document.querySelectorAll("[data-help-toggle]");
    const notificationLayer = document.querySelector("[data-notifications]");
    const notificationTriggers = document.querySelectorAll("[data-notification-toggle]");
    let showNotificationBadge = true;
    let latestUnreadCount = 0;
    const notificationPreference = document.querySelector("[data-notification-preference]");
    if (notificationPreference) {
        const preferenceKey = notificationPreference.dataset.storageKey;
        try {
            const savedPreference = window.localStorage.getItem(preferenceKey);
            if (savedPreference !== null) showNotificationBadge = savedPreference !== "false";
        } catch (_) { /* Keep the default preference if browser storage is unavailable. */ }
        notificationPreference.checked = showNotificationBadge;
        notificationPreference.addEventListener("change", () => {
            showNotificationBadge = notificationPreference.checked;
            const status = document.querySelector("[data-notification-preference-status]");
            try {
                window.localStorage.setItem(preferenceKey, String(showNotificationBadge));
                if (status) status.textContent = "Saved on this browser.";
            } catch (_) {
                if (status) status.textContent = "This browser could not save the preference.";
            }
            document.querySelectorAll("[data-notification-badge]").forEach((badge) => {
                badge.hidden = !showNotificationBadge || latestUnreadCount === 0;
            });
            notificationTriggers.forEach((trigger) => trigger.setAttribute("aria-label", latestUnreadCount ? `Notifications, ${latestUnreadCount} unread` : "Notifications"));
        });
    }
    let activeUtilityTrigger = null;
    const closeUtilityLayers = (restoreFocus = false) => {
        [helpLayer, notificationLayer].filter(Boolean).forEach((layer) => { layer.hidden = true; });
        helpTriggers.forEach((trigger) => trigger.setAttribute("aria-expanded", "false"));
        notificationTriggers.forEach((trigger) => trigger.setAttribute("aria-expanded", "false"));
        if (restoreFocus && activeUtilityTrigger) activeUtilityTrigger.focus();
    };
    const showUtilityLayer = (layer, trigger) => {
        const wasOpen = !layer.hidden;
        closeUtilityLayers();
        if (accountMenu) {
            accountMenu.hidden = true;
            document.querySelectorAll("[data-account-menu-toggle]").forEach((item) => item.setAttribute("aria-expanded", "false"));
        }
        if (wasOpen) {
            if (trigger) trigger.setAttribute("aria-expanded", "false");
            return;
        }
        activeUtilityTrigger = trigger;
        layer.hidden = false;
        if (trigger) trigger.setAttribute("aria-expanded", "true");
        const close = layer.querySelector(".utility-close");
        if (close) close.focus();
    };

    if (helpLayer) {
        const helpPages = [...helpLayer.querySelectorAll("[data-help-page]")];
        const showHelpPage = (pageNumber) => {
            helpPages.forEach((page) => { page.hidden = page.dataset.helpPage !== String(pageNumber); });
            helpLayer.dataset.page = String(pageNumber);
            const panel = helpLayer.querySelector(".help-panel");
            if (panel) panel.scrollTop = 0;
            const currentPage = helpPages.find((page) => page.dataset.helpPage === String(pageNumber));
            if (currentPage) currentPage.focus();
        };
        helpTriggers.forEach((trigger) => trigger.addEventListener("click", () => {
            showHelpPage(1);
            showUtilityLayer(helpLayer, trigger);
        }));
        helpLayer.querySelectorAll("[data-help-next]").forEach((button) => button.addEventListener("click", () => showHelpPage(2)));
        helpLayer.querySelectorAll("[data-help-back]").forEach((button) => button.addEventListener("click", () => showHelpPage(1)));
        helpLayer.querySelectorAll("[data-help-close]").forEach((button) => button.addEventListener("click", () => closeUtilityLayers(true)));
        helpLayer.querySelectorAll("a[href]").forEach((link) => link.addEventListener("click", () => closeUtilityLayers()));
    }

    if (notificationLayer) {
        const content = notificationLayer.querySelector("[data-notification-content]");
        const countLabel = notificationLayer.querySelector("[data-notification-total]");
        const badgeElements = document.querySelectorAll("[data-notification-badge]");
        const storageKey = notificationLayer.dataset.storageKey;
        let hasReadBaseline = false;
        let readKeys = new Set();
        try {
            const saved = window.localStorage.getItem(storageKey);
            if (saved !== null) {
                const parsed = JSON.parse(saved);
                if (Array.isArray(parsed)) readKeys = new Set(parsed);
                hasReadBaseline = true;
            }
        } catch (_) { /* Notifications still work if browser storage is unavailable. */ }

        const saveReadKeys = () => {
            try { window.localStorage.setItem(storageKey, JSON.stringify([...readKeys].slice(-100))); }
            catch (_) { /* Read state is a convenience; the activity feed remains available. */ }
        };
        const formatActivityTime = (value) => {
            const parsed = new Date(String(value).replace(" ", "T").replace(/(\.\d{3})\d+$/, "$1"));
            if (Number.isNaN(parsed.getTime())) return value;
            const age = Math.max(0, Date.now() - parsed.getTime());
            if (age < 60000) return "Just now";
            if (age < 3600000) return `${Math.floor(age / 60000)} min ago`;
            if (age < 86400000) return `${Math.floor(age / 3600000)} hr ago`;
            if (age < 604800000) return `${Math.floor(age / 86400000)} days ago`;
            return new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(parsed);
        };
        const updateReadPresentation = () => {
            const items = [...notificationLayer.querySelectorAll(".notification-item[data-notification-key]")];
            let unread = 0;
            items.forEach((item) => {
                const isUnread = !readKeys.has(item.dataset.notificationKey);
                item.classList.toggle("is-unread", isUnread);
                if (isUnread) unread += 1;
            });
            latestUnreadCount = unread;
            countLabel.textContent = unread ? `${unread} new` : "";
            badgeElements.forEach((badge) => {
                badge.hidden = !showNotificationBadge || unread === 0;
                badge.textContent = unread > 9 ? "9+" : String(unread);
            });
        };
        let loadingNotifications = false;
        const loadNotifications = async () => {
            if (loadingNotifications || !content) return;
            loadingNotifications = true;
            try {
                const response = await fetch(notificationLayer.dataset.endpoint, {
                    headers: { "Accept": "text/html" },
                    credentials: "same-origin",
                    cache: "no-store"
                });
                if (!response.ok) throw new Error("Notifications could not be loaded. Please try again.");
                const markup = await response.text();
                const dashboard = new DOMParser().parseFromString(markup, "text/html");
                if (dashboard.querySelector(".login-page")) throw new Error("Your session expired. Sign in again to view activity.");
                const list = document.createElement("div");
                list.className = "notification-list";
                list.dataset.notificationList = "";
                const events = [...dashboard.querySelectorAll(".dashboard-event")];
                events.forEach((event) => {
                    const heading = event.querySelector("strong")?.textContent.trim() || "Workspace update";
                    const [type, ...titleParts] = heading.split(/\s+\|\s+/);
                    const title = titleParts.join(" | ") || heading;
                    const detail = event.querySelector("p")?.textContent.trim() || "";
                    const createdAt = event.querySelector("time")?.textContent.trim() || "";
                    const item = document.createElement("article");
                    item.className = "notification-item";
                    item.dataset.notificationKey = [type, title, detail, createdAt].join("|");
                    const mark = document.createElement("span");
                    mark.className = "notification-mark";
                    mark.setAttribute("aria-hidden", "true");
                    mark.innerHTML = '<svg viewBox="0 0 20 20" focusable="false"><path d="M4 15.5 5.2 12 13 4.2a1.7 1.7 0 0 1 2.4 2.4l-7.8 7.8L4 15.5Z"/><path d="m11.8 5.4 2.8 2.8M4.5 18h11"/></svg>';
                    const copy = document.createElement("div");
                    copy.className = "notification-copy";
                    const titleRow = document.createElement("div");
                    titleRow.className = "notification-title-row";
                    const typeLabel = document.createElement("strong");
                    typeLabel.textContent = type;
                    const unreadDot = document.createElement("span");
                    unreadDot.className = "notification-unread-dot";
                    unreadDot.setAttribute("aria-label", "Unread");
                    titleRow.append(typeLabel, unreadDot);
                    const titleText = document.createElement("p");
                    titleText.textContent = title;
                    const detailText = document.createElement("small");
                    detailText.textContent = detail;
                    const time = document.createElement("time");
                    const timestamp = new Date(String(createdAt).replace(" ", "T").replace(/(\.\d{3})\d+$/, "$1"));
                    if (!Number.isNaN(timestamp.getTime())) time.dateTime = timestamp.toISOString();
                    time.textContent = formatActivityTime(createdAt);
                    copy.append(titleRow, titleText, detailText, time);
                    item.append(mark, copy);
                    list.append(item);
                });
                if (!events.length && dashboard.querySelector(".activity-zero")) {
                    const empty = document.createElement("div");
                    empty.className = "notification-empty";
                    empty.innerHTML = '<span class="notification-empty-icon" aria-hidden="true">OK</span><strong>You\'re all caught up</strong><p>New project, dataset, experiment, and training activity will show up here.</p>';
                    list.append(empty);
                }
                if (!events.length && !dashboard.querySelector(".activity-zero")) throw new Error("Notifications could not be loaded. Please try again.");
                content.replaceChildren(list);
                if (!hasReadBaseline) {
                    hasReadBaseline = true;
                    saveReadKeys();
                }
                updateReadPresentation();
            } catch (error) {
                if (!content.querySelector("[data-notification-list]")) {
                    const message = document.createElement("p");
                    message.className = "notification-loading notification-error";
                    message.textContent = error.message || "Notifications are temporarily unavailable.";
                    content.replaceChildren(message);
                }
            } finally {
                loadingNotifications = false;
            }
        };

        notificationTriggers.forEach((trigger) => trigger.addEventListener("click", () => {
            showUtilityLayer(notificationLayer, trigger);
            if (!notificationLayer.hidden) loadNotifications();
        }));
        notificationLayer.querySelectorAll("[data-notifications-close]").forEach((button) => button.addEventListener("click", () => closeUtilityLayers(true)));
        notificationLayer.querySelector("[data-mark-notifications-read]").addEventListener("click", () => {
            notificationLayer.querySelectorAll(".notification-item[data-notification-key]").forEach((item) => readKeys.add(item.dataset.notificationKey));
            saveReadKeys();
            updateReadPresentation();
        });
        notificationLayer.querySelectorAll(".notification-footer a[href]").forEach((link) => link.addEventListener("click", () => closeUtilityLayers()));
        loadNotifications();
        window.setInterval(loadNotifications, 60000);
    }

    document.addEventListener("keydown", (event) => {
        if (event.key !== "Escape") return;
        const utilityOpen = (helpLayer && !helpLayer.hidden) || (notificationLayer && !notificationLayer.hidden);
        if (utilityOpen) {
            event.preventDefault();
            closeUtilityLayers(true);
        }
    });

    document.querySelectorAll("form[data-confirm]").forEach((form) => {
        form.addEventListener("submit", (event) => {
            if (!window.confirm(form.dataset.confirm)) {
                event.preventDefault();
            }
        });
    });
});
