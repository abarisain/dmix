## MPDroid

**MPDroid** is a MPD client for Android. It is a fork of [PMix](http://code.google.com/p/pmix/).

You can browse your library, control the current song and playlist, manage your outputs, and stream music right to your mobile device. And all of this wrapped up in a beautiful modern Holo design!

What's not to like?

![Now Playing/library Screenshot](https://raw.github.com/abarisain/dmix/master/Screenshots/readme.png)  

#### Compatibility

MPDroid works on **all** devices from 4.0.3 (API 15) to 4.4+

[![Get it on F-Droid](get_it_on_f-droid.png?raw=true)](https://f-droid.org/repository/browse/?fdid=com.namelessdev.mpdroid)

#### Libraries

- JMPDComm - The core MPD interface lib. Heavily modified.
- DragSortListView - Really nice and widely used TouchInterceptor replacement.
- ActionBar-PullToRefresh - Pull to refresh for library.  

## Known issues

- Limited multi server support (based on WLAN name)

## Roadmap

New features will most likely be delayed. MPDroid is, in my opinion, almost fully-featured and pushes (hits) MPD's API limits. Widely requested features (like better search, better library browsing) can't be done without duplicating MPD's database locally. This is a huge project, and it will take a lot of time (if ever done).

Also, MPDroid's speed isn't that great, but considering that MPD's API was never made for 3G (read crappy) connections, it is also not that bad.

So, the current roadmap is:

####1.1  

 - [ ] Make MPDroid configurable for multiple servers (Better implementation than the hackish WLAN based one)
 - [ ] Add Bonjour support
 - [ ] Rework Streams storage
 - [ ] Split album details by CD and allow only one CD to be added to the playlist

## Donations

Since MPDroid is a fork of dmix and that a lot of people helped me with some huge patches (see special thanks), I don't really believe it would be ethical to accept donations.

If you still want to donate, I have a PayPal address at dreamteam69@gmail.com, you can send how much you want to it :)

Please consider donating to some of the developers down there :)

## Special thanks

Obviously, PMix for their work on the original application. There is not much of PMix left, but their core code is still here.

Everybody who blogged about MPDroid, allowing it to gain a large user base.
Mopidy, for working with me on the search support.

#### Developers

There are a lot of people here. Developers who crossed my path now and then, working on MPDroid and fixing some things. This project wouldn't be the same without you!

[Kent Gustavsson](https://github.com/orrche) - Cleaned up a lot of PMix's old bugged core and polished the interface. Huge refactoring work on the activities.

[Phil Chandler](https://github.com/philchand) - Various fixes and improvements to internal stuff. Added more options to the way it handles the library.

[Guillaume Revaillot](https://github.com/grevaillot) - Tablet layout work

Andrew Blackburn - Various stuff, suggestions and layout work

[Stefan Richter](https://github.com/02strich) - Refactoring and … Widget ! I was really too lazy about this one, and it was a much requested feature. Also added a way to build MPDroid from the command line, which I didn't even try.

[Matt Black](https://github.com/mafrosis) - Also worked on ant building. Reported a log of bugs/improvement suggestions. We may not agree on everything but it's been useful!

Craig Drummond - Helped me integrate new features while cleaning up old internal code that I did not really wanted to change. Lots of new interface code and feedback on my experimentations while reworking the whole interface.

[jcnoir](https://github.com/jcnoir) - Contributor. Reworked cover art support, and many other improvements.

[hurzl](https://github.com/hurzl) - MPD Backend improvement (cached MPD), various tweaks and bug fixes.

[Avuton Olrich](https://github.com/avuton) - Behind the scenes fixes, cleanup and Git support.

Other patch submitters : Jörg Thalheim, Florian Weile, Daniel Schoepe, John Bäckstrand, ...

And if I forgot you… I'm sorry, it's kinda hard to remember everybody who worked on a 2 year old project, feel free to mail me :)


Thanks a lot guys!
