h1. github-away
How to set up Git and GitHub on a foreign machine.

Go to https://github.com/settings/tokens and generate a new token.
```shell
git clone https://...
# enter username twisterrob and token as password
git config --local --add user.email papp.robert.s@gmail.com
git config --local --add user.name "Róbert Papp (TWiStErRob)"
```
