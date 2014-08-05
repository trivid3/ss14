Space Station 14
====
***!!!unplayable as of 05/08/2014 pleasewait as i'm trying to fix everything!!!***

Initially intended as a joke and as a way for me to learn networking, this is a remake of space station 13 written in blitz3D

How to play
1) Download blitz3D at the official website
2) Compile server.bb, this will create a local server at the port 1234. Edit server.bb to set it to another port
3) Compile client.bb and you'll become a spaceman. By default it connects to 127.0.0.1:1234, edit client.bb to change that

===

How to contribute
1) Download blitz3D at the official website
2) Contribute

understanding this mess

server.bb and client.bb share all other files in the home directory (except for "graphics.bb" and "fastImage.bb", which is only used by the client)

network_defines.bb - shit to make working with networking easier

FastImage (bb & dll)- a library made by some drunk russians that improves blitz3d's ability to work with 2d

pregame.bb - map initialising and so on

items.bb - items, clothing, etc

other_defines.bb - contains everything else

current problems

-moving is broken

-that library made by some drunk russians costs like 10$ dollars but i didn't pay them so a pop-up window saying 'trial version' will sometimes interrupt one's playing

-lack of gui stuff like text fields, areas and buttons. there is a library for that, so the problem won't last long once I get it

-no layering - map is always drawn first, then objects like windows, then items, then mobs. if two things are on a same tile, the thing created later will be drawn on the top. The latter is possible to change pretty easily, but the former, however, will require a lot of witchcraft

-the name generator works like shit

-same could be said about the rndtext$ function

-and blitz3d
