# uChat
A simple, mediocre Java app for chatting with people on the same LAN

---
Originally created between classes in college. I decided to clean the code up a bit (but not *too* much) and post it here.

## "Features"
  * screen names with absolutely no authentication
  * UDP broadcast messaging for "peer-to-peer" operation
  * mitigation for at least one possible exploit (see the `scrubMesage(String)` method)
