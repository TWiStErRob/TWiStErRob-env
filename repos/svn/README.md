> This is a Subversion repository; use the 'svnadmin' tool to examine it.
> Do not add, delete, or modify files here unless you know how to avoid corrupting the repository.
>
> Visit http://subversion.tigris.org/ for more information.

# Background
BDB slower but more recoverable, FSFS is much faster and dynamic. [1]
You can use svn:externals to tell svn client to pull data from another repository, relative paths [3] are introduced in 1.5 [4]
I'm using single-solution model [2] for my project, linking common projects to more solutions is done by svn:externals.

# Naming
Projects names should be the following in a bigger solution:

| Project          | Description                                                                       |
|------------------|-----------------------------------------------------------------------------------|
| Main Application | builds to SolutionName.exe as a runnable application                              |
| Model            | has the data model classes                                                        |
| Data Access      | manages the model from various data sources                                       |
| TWiStEr.*        | reusable project references                                                       |
| <Folder>         | Solution folders can be used to further group projects (eg. AddIn System, AddIns) |

# Structure
Repository tree
```
	+-Build Utilities
	|	+-nUnitx.xx
	|	+-ant.yyy
	+-Utilities
	|	+-WPFColors
	|	+-ExtensionEnumerators
	|	+-SearchAndReplace
	+-Libraries
	|	+-TWiStEr.Extensions
	|	+-TWiStEr.Data
	+-Projects
	|	+-Path Finder
	|	|	+-TWiStEr.Extensions (svn:externals=/Libraries/TWiStEr.Extensions)
	|	|	+-Path Finder.Algos
	|	|	+-Path Finder.GUI
	|	+-ETR Parser
	|		+-TWiStEr.Extensions (svn:externals=/Libraries/TWiStEr.Extensions)
	|		+-ETRParser.Data
	|		+-ETRParser.Application
	+-Examples
		+-FreeGlutExamples
		+-TestCollections
```

Project hierarchy
```
	?-bin
	|	+-Debug
	|	|	*.exe
	|	+-Release
	|		*.exe
	?-obj
	|	+-Debug
	|	|	*.g.cs
	|	+-Release
	|		*.g.cs
	+-Properties	
		*.cs
	*.cs
	ProjectName.xxproj
	ProjectName.sln
```

Multiproject hierarchy
```
	+-Project1
	+-Project2
	+-Project3
	?-output
	build.script
	ProjectName.sln
```

# Filtering
```bat
set repo=P:\repos\svn
set temp_repo=P:\temp\temp_svn
set filter_args=exclude Projects/Sensor3D/tmp
svnadmin create "%temp_repo%" 2>&1 >1_create.log && svnadmin dump --quiet "%repo%" 2>2_dump.log | svndumpfilter %filter_args% 2>3_filter.log | svnadmin load "%temp_repo%" 2>&1 >4_load.log
```

# Footnotes
 * [1] http://svnbook.red-bean.com/nightly/en/svn.reposadmin.planning.html#svn.reposadmin.basics.backends.tbl-1
 * [2] http://msdn.microsoft.com/en-us/library/ms998208.aspx
 * [3] http://svn.haxx.se/dev/archive-2007-07/0509.shtml
 * [4] http://svnbook.red-bean.com/en/1.5/svn.advanced.externals.html
