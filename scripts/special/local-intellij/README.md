# IntelliJ Local executor

These .bat files allow execution of Android Studio and IntelliJ IDEA with no side defects on the machine.
It's very useful when wanting to try something on a different (previous, EAP, alpha, beta) version of IntelliJ products.

Call it like this:
```
local-idea.bat C:\full\path\to\IntelliJ-IDEA
# or
local-studio.bat C:\full\path\to\AndroidStudio
```

## How does it work?
When executed the `local.properties` will relocate all the relevant folders into the `local` directory inside the IntelliJ product's installation folder.
This means that for debugging, reproducing, testing, starting over is as simple as deleting the `local` folder.

For best effect download the .ZIP version without an installer, so when the installation is removed (by deleting the folder) all the associated files are also removed.

## Inheriting system settings
Comment out the `_VM_OPTIONS` variable in the `.bat` files to use the system environment variable if that's set up.

## Direct execution
If you want to avoid calling the `idea.bat` or `studio.bat` files in the `bin` directory, you can execute directly with:
```
idea64.exe -Didea.properties.file=C:\full\path\to\local.properties
# or
studio64.exe -Didea.properties.file=C:\full\path\to\local.properties
```
