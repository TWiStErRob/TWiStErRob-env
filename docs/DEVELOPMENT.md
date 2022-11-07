# Development setup
This file summarizes the steps to setup a development environment for projects in this repository and may apply to other repositories too.

## Kotlin Scripts

1. Open folder in IntelliJ IDEA
2. Open `....main.kts`
3. _Apply Context_ if necessary
4. If code is red
    * _File > Project Structure..._
    * _Project Settings / Project_
      * _SDK_ select _Java 11+_
      * _Language Level_ select _SDK default_

Then _right click_ the `....main.kts` file to run the script.

Usually without any more setup it'll output what else is missing:
```
Usage: kotlinc -script ....main.kts ...
```

Edit the Kotlin script Run Configuration, usually it'll need some of these:
* Environment variables: `X=x;Y=y`
* Program arguments: `-a "b c" -d e`
