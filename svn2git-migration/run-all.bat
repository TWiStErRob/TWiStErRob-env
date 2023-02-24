@rem Run each in parallel in new windows.
start "BLT" cmd /K "run.bat monorepo-split-net.twisterrob.travel.rules -travel 1 10000"
start "Cinema" cmd /K "run.bat net.twisterrob.cinema.rules -cinema 1 10000"
start "ColorFilters" cmd /K "run.bat net.twisterrob.colorfilters.rules -color 1 10000"
start "Gradle" cmd /K "run.bat net.twisterrob.gradle.rules -gradle 1 10000"
start "Inventory" cmd /K "run.bat monorepo-split-net.twisterrob.inventory.rules -inventory 1 10000"
start "Others" cmd /K "run.bat monorepo-split-independent_projects.rules -all 1 10000"
start "Web/Tests" cmd /K "run.bat net.twisterrob.healthcheck.rules -hc 2645 2656"
@rem They'll stay open when finished, but the docker container will stop.
