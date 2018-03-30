# mkblock
Blockchain(Server)
- This blockchain came from Codepace, which was not working. So i gotta hard time to modify many things and added client on it.
- for test, you should run "mkblock" on 2 OS. then you can watch to be synced blocks each other.   
- Let's me know, if someone got hard time like me. i can give to you some help. :)

you have to install 2 things.
1. mkblock for server
2. mkc for client

Built With
- Intellij IDEA
- Gradle

How to Run
- Install Java 1.8.0 or higher
- Download mkblock from github.com : git clone https://github.com/maior/mkblock.git
- Modify your ip-address in "Constants.java": ex) "192.168.0.4:8787"
- Run>>"Run Main"
- if you wanna run mkblock.jar file, click "File>>Project Structure..." in menu.

![ex_screenshot](./img/sc2.png)

- Please, check "Include in project build"
- make build : "Build>>Rebuild Project"
- move to "cd ./out/artifacts/mkblock_jar"
- Run "java -cp mkblock.jar mkii.mkblock.Main"
- If you wanna run console mode : Run "java -cp mkblock.jar mkii.mkblock.Main console"
- If you have console mode, Run ">> getinfo" or ">> getnewaccount" then it could be displied account values.
<br>
- added smart? contract<br>
- refer "mkweb" and "mkclient"

Author
- Kenneth Kwon - maiordba@gmail.com

License
mkblock Core is released under the terms of the MIT license.
