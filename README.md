# Nanit Takehome

I tried to keep the project very simple. I've mostly only used 1st party dependencies and very straightforward APIs and patterns. 

- Architecture mostly follows latest Google guidance, including Navigation3
- DI is manual, with lazy vals. There's no need for activity-scoped dependencies or anything complex, so Hilt, etc. feels like overkill
- Coil was used to load the file from disk, which might be slight overkill but very straightforward and common use-case
- Share button wasn't in Figma but it was on the iOS page, which I tweaked a bit to make things look better on the background
- Share capture took compose snapshot and used a technique to temporarily hide views that don't get shared
- Claude was used in a very step-by-step, extremely controlled manner and reviewed carefully. I've included the guidance given to Claude for context
- Added detekt with minimal config for keeping project organized and tidy
- Suppressed backups for privacy

Things I did not invest in that I would look at more in a real app:

- Did not test accessibility although tried to be mindful of it when developing
- Dark mode / improve dynamic theme support
- API 24-25 may not support the app icon or share features properly. Min sdk 26 feels defensible but just left this for now

Thanks!