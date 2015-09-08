ImmortalPlayer 3.5 (audio/video, HTTP/FTP/NMDC (peering))
==============

Complex logic, but it's simplest way to organize player with cache on sdcard and with play in background.

Features:

1. Based on standart player. Easy to use with different other player, but! Not recommended;
2. Support protocols: http, ftp, nmdc;
3. One thread to read, send and save to cache data;
4. Most simplest way and most fastest work;
5. Play in background without using service e.t.c.
6. Small size of code, no need to update player component
7. Ready for deploy to any program. No need to use library.
8. The player starts to play a video from a local file (if exist) and not from the internet.
9. The player plays the corrupted files.
10. Player API 16+, Proxy API 14+.
11. Automatically search and streaming current file from peering networks!
12. DoubleDomain, DoubleBuffer and Dynamic Network technologies.

Try from play market Android 4.1.1+ (api:16+): https://play.google.com/store/apps/details?id=com.immortalplayer

Known programs work on ImmortalPlayer:
https://play.google.com/store/apps/details?id=com.medialibrary.mycollection

New technologies (Description):

<b>DoubleDomain</b> - To ensure uninterrupted access to the site are used the second domain. 
How it works - when there is no access to the site A program starts to work with the site B. The list of relevant sites specified in the program or on a special website. For the user, this technology is transparent, and it always seems to open the site A

<b>DoubleBuffer</b> - one buffer for two. With simultaneous playback and downloading the same file, if the downloaded part more than you need to play, it uses local part of the downloaded file to play the media.

<b>Dynamic Network</b> - enabling and disabling 2G \ 3G \ 4G manages one program. Enabling network occurs only when necessary to transfer data. This greatly reduces power consumption and electromagnetic radiation from the device.
