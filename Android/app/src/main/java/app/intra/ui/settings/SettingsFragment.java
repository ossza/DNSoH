    new AlertDialog.Builder(getContext())
        .setTitle("Control setup")
        .setMessage("1. Set a PIN below so the service cannot be turned off without it.\n\n" +
            "2. Leave Secure DNS ON. It restarts after reboot if the phone allows autostart.\n\n" +
            "3. On Huawei: Settings → Battery → Launch Manager. Search for Secure DNS, turn the switch off, then enable Auto-launch, Secondary launch and Run in background.\n\n" +
            "4. Settings → More Connections → VPN. Make sure Secure DNS is not switched off.\n\n" +
            "Some phones (Huawei, Xiaomi, Samsung) also need autostart allowed in the phone's app settings.")
        .setView(pinInput)