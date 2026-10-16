        // Make website link clickable.
        serverWebsite.setMovementMethod(LinkMovementMethod.getInstance());

        // Make portal hint link clickable.
        TextView portalHint = view.findViewById(R.id.portal_hint);
        if (portalHint != null) {
          portalHint.setMovementMethod(LinkMovementMethod.getInstance());
        }