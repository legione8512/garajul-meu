# Google Play — Apply for production: draft answers

Drafted 2026-09-23, for the application that opens around **2026-10-07**, when the
closed test (12 testers opted in on 2026-09-23) completes its fourteen days.

The question wording below is the usual shape of the form for new personal
developer accounts, written from memory. **Check it against the console on the
day** and adjust the answers to the questions actually asked. Answers are in
English, as the console is.

**Still to do before sending:** collect two or three concrete comments from
testers (what was most useful, where they hesitated, what they would add) and
work them into Part 1 and Part 3. An application whose feedback reads only
"everything was fine" is sometimes refused as evidence of too little testing.
Suggested message to the testers, in Romanian:

> Salut! Mulțumesc că testezi Garajul Meu 🙏 Am nevoie de 2 minute de la tine, pentru cererea către Google:
> 1. Ce ți-a fost cel mai util în aplicație?
> 2. Ai avut vreun moment în care n-ai știut unde să apeși sau ce să faci?
> 3. Ce ai vrea să mai facă aplicația?
> Orice răspuns ajută, chiar și mic. Mersi!

No tester's name or address belongs in this file: the repository is public.

---

## Part 1 — About your closed test

**How easy was it to recruit testers for your app?**

> Moderately easy. I recruited 12 testers from friends, family and colleagues. Two addresses were initially rejected by the console because they were not the Google accounts used on their phones; I collected the correct ones. One tester's Play country was Germany, so I added Germany to the test track's countries.

**Describe the engagement you received from testers during your closed test**

> All 12 testers installed the app, created an account, verified their email, and added their own vehicles and documents (insurance, technical inspection, road tax). They used the app on their own phones throughout the test period and received the expiry reminders as push notifications. Two app updates (1.0.1 and 1.0.2) were delivered to them through the test track during this time.

**Provide a summary of the feedback you received from testers and how you collected it**

> Feedback was collected directly from each tester through messages and conversations. It was consistently positive: testers found the app easy to use and useful, particularly the overview of what expires and when, and the reminders. No crashes or functional problems were reported. Alongside tester feedback, I tested the app myself on several real devices (Android 7, 9 and newer) and fixed the issues I found (see the changes below).

## Part 2 — About your app

**Who is the intended audience of your app?**

> Car owners in Romania who want to keep track of their vehicle documents and their expiry dates: mandatory insurance (RCA), technical inspection (ITP), road tax (rovinietă), optional CASCO insurance, fire extinguisher and first-aid kit.

**Describe how your app provides value to users**

> Missing an expiry in Romania means fines or driving uninsured. The app keeps every vehicle and its documents in one place, shows at a glance what is valid, what expires soon and what has expired, and sends push reminders 30, 14, 7, 3 and 1 days before expiry and on the day itself. A vehicle can be added by photographing the registration certificate. The app is in Romanian by default, with English available.

**How many installs do you expect your app to have in its first year?**

> About 1,000 — choose the range that contains it if the form offers ranges.

## Part 3 — About your production readiness

**What changes did you make to your app based on what you learned during your closed test?**

> Two updates were released to the closed track during testing.
> 1.0.1: corrected Romanian plural forms in the document states ("1 zile" → "mâine", "20 zile" → "20 de zile") and handled undeliverable email addresses at registration with a clear message instead of a server error. Both were found during testing of the first release.
> 1.0.2: a dedicated notification channel with a Romanian name, Romanian as the default language regardless of the phone's language, redesigned tab navigation, tracking of the fire extinguisher and first-aid kit, the vehicle's usage type, and the owner's vehicle photo on the dashboard cards.

**How did you decide that your app is ready for production?**

> 12 testers used the app on their own phones for more than 14 days with no crashes or functional problems reported. Reminders are delivered in production both in the background and in the foreground. The backend has 387 automated tests and the frontend 409, run automatically on every change. The same app has been live on the Apple App Store since September 2026.

Update the test counts from `PROJECT_STATE.md` if they have changed by the day.
