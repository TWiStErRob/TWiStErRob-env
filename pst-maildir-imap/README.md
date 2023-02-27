PST to Maildir to IMAP
======================

# Prerequisites

 * Outlook 2010 (Source: https://answers.microsoft.com/en-us/msoffice/forum/all/check-download-links-for-office-2010/34d50099-139d-47fe-b8b9-2aa05528e90d)
   * Office 2010 Single Image x86 (Home and Student, Home and Business, Professional)  
     http://web.archive.org/web/20121031215918/http://msft.digitalrivercontent.net/office2010/X16-32007.exe
     http://web.archive.org/web/20140326190202/http://azcdn01.digitalrivercontent.net/office2010/X17-75058.exe
   * Office 2010 Single Image x64 (Home and Student, Home and Business, Professional)  
     http://web.archive.org/web/20121101022201/http://msft.digitalrivercontent.net/office2010/X16-31970.exe
     http://web.archive.org/web/20140104170411/http://msft.digitalrivercontent.net/office2010/X17-75161.exe  
     X17 is 2010 SP1, the difference beween the two is in the "Updates" folder: everything except README.txt.
   * Office 2010 Single Image x86 + x64 disk image (Home and Student, Home and Business, Professional)  
     http://azcdn01.digitalrivercontent.net/office2010/X16-31933.iso (not archive, and not hosted any more)
 * `readpst` installed with `apt install pst-utils`  
    ```console
    ~$ readpst -V
    ReadPST / LibPST v0.6.71
    Little Endian implementation being used.
    ```
 * `mbox2maildir.pl` (from https://github.com/porwat/PST_to_Maildir)  
   can be interpreted with Perl v5.26.1 or compatible.
 * `fix_maildir_mail_mtime.py` (from https://wiki.archlinux.org/title/Isync#Emails_on_remote_server_have_the_wrong_date)
   can be interpreted with `pyhton3` which was installed in WSL Ubuntu
    ```console
    ~$ python3 --version
    Python 3.6.8
    ```

Note: Windows Explorer / Total Commander can see WSL Home in: `\\wsl$\Ubuntu\home\`

# References
 * Maildir format: http://cr.yp.to/proto/maildir.html
 * `mbsync` docs: https://isync.sourceforge.io/
 * `mbsync` man: https://isync.sourceforge.io/mbsync.html
 * Date fix: https://wiki.archlinux.org/title/Isync#Emails_on_remote_server_have_the_wrong_date
 * Amazing practical description: https://wiki.archlinux.org/title/Isync
 * Get away from Google articles:
   * https://www.jonatkinson.co.uk/posts/syncing-gmail-with-mbsync/
   * https://jakewharton.com/removing-google-as-a-single-point-of-failure-gmail/  
   https://github.com/JakeWharton/docker-mbsync

# Process

 1. [Compact PST file in Outlook](https://support.microsoft.com/en-us/office/reduce-the-size-of-your-mailbox-and-outlook-data-files-pst-and-ost-e4c6a4f1-d39c-47dc-a4fa-abe96dc8c7ef#ID0EBBD=Office_2010)
 1. Exit Outlook to make sure pst file lock is released and data written.  
    Make sure it's not on the system tray.
 1. Convert a PST file to a big `.mbox` file:
    ```bash
    readpst -o test/ test/test.pst
    ```
 1. Split up the bix .mbox file into smaller ones:
    ```bash
    perl ./mbox2maildir.pl test/Inbox.mbox test/Inbox/
    ```
 1. Fix the timestamps with:
    ```bash
    find mail -type f -exec python3 fix_maildir_mail_mtime.py "{}" \;
    ```
 1. Set up mbsync.rc
 1. `export MBSYNC_PASSWORD=...`
 1. Run `mbsync --config mbsync.rc --push gmail-channel`

You can create a backup of existing data on IMAP with:
```bash
mbsync --config mbsync.rc --pull gmail-channel
```

If something went sideways, it's easy to restart:
 * Delete all email in `inbox`
 * Empty `Trash`
 * `--push` again

# Issues

## 443 of 1832 emails pushed
```console
C: 1/1  B: 1/2  M: +443/1832 *0/0 #0/0  S: +0/0 *0/0 #0/0
```
This is because of two reasons:
 * timeout: default is 20 seconds, and 25MB emails might not complete.
 * limit: GMail has a 25MB limit (https://support.google.com/mail/answer/6584#zippy=%2Cattachment-size-limit)  
          because of base64, even a 16MB attachment is 24MB (https://www.gmass.co/blog/gmail-email-size-limit/)  
          when mbsync encounters a big file GMail doesn't accept, it errors with:
          ```
          IMAP command 'APPEND "INBOX" (\Seen) "18-Jun-2017 13:16:13 +0100" ' returned an error: BAD [TOOBIG] Message too large. https://support.google.com/mail/answer/6584#limit
          ```
          and terminates the sync.

## `BAD [TOOBIG] Message too large.`
Some emails have the same attachment multiple times and this results in bigger file sizes. This might be because of readpst or Outlook doing something weird.
