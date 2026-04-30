$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.2"
$m2 = "C:\Users\hys\project\apache-maven-3.9.9"
$args = $args -join ' '
$cmd = "& `"$env:JAVA_HOME\bin\java`" -cp `"$m2\boot\plexus-classworlds-2.8.0.jar`" `"-Dclassworlds.conf=$m2\bin\m2.conf`" `"-Dmaven.home=$m2`" `"-Dmaven.multiModuleProjectDirectory=$pwd`" org.codehaus.plexus.classworlds.launcher.Launcher $args"
Invoke-Expression $cmd
