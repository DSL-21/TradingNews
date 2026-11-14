# Trading News — Privacy Policy Draft Template

This is a template, not a final legal document. Replace every bracketed field and have the completed policy reviewed before public distribution.

**Operator:** [LEGAL NAME / BUSINESS NAME]  
**Contact:** [SUPPORT EMAIL]  
**Last updated:** [DATE]

## Information processed

The app does not require an account. It stores a compact local cache of public headline metadata, the user's selected instrument watchlist, and refresh/alert preferences in Android app-private storage. Optional background refresh uses Android WorkManager. Optional local headline notifications require Android notification permission on Android 13 and later. The app does not request location, contacts, microphone, or camera permissions. It does not include advertising or analytics SDKs in this version.

## Network requests

When the user opens or refreshes the news feed, the app contacts the GDELT DOC 2.0 API. The selected instrument watchlist is applied locally and is not sent to GDELT. Opening a headline launches the original publisher website in the user's browser. Opening the Economic Calendar launches the official Forex Factory website. GDELT, publishers, the browser, Forex Factory, and network providers may process connection metadata under their own policies.

## Purposes and legal basis

[DESCRIBE THE PURPOSES OF PROCESSING AND APPLICABLE LEGAL BASES UNDER THE LAWS THAT APPLY TO THE OPERATOR AND USERS.]

## Local storage and retention

The app stores the most recent successful GDELT headline metadata response, its retrieval time, selected watchlist, refresh preferences, and alert preferences on the device. Cached news data older than 24 hours is discarded. [CONFIRM WHETHER CACHE REMOVAL ON APP UNINSTALL IS SUFFICIENT FOR YOUR RETENTION POLICY.]

## Notifications

Optional headline notifications are generated locally on the device by WorkManager when the user enables alerts and grants notification permission where required. They are not remote Firebase push messages. Android battery and background-execution policies can delay or suppress them.

## Sharing and third parties

[DESCRIBE THIRD-PARTY PROVIDERS, THEIR ROLES, AND WHETHER ANY DATA IS SHARED BEYOND ORDINARY NETWORK REQUEST METADATA.]

## User rights and contact

[DESCRIBE APPLICABLE USER RIGHTS, HOW TO MAKE A REQUEST, AND THE SUPPORT CONTACT PROCESS.]

## Security

[DESCRIBE RELEASE SIGNING, SECURITY UPDATES, INCIDENT HANDLING, AND ANY HOSTING PROVIDERS.]

Do not publish this template unchanged. The operator must supply real legal identity, support contact, jurisdiction-specific disclosures, and accurate store declarations.
