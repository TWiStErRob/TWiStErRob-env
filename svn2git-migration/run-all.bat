@rem Run each in parallel in new windows.
start "BLT" cmd /K "run.bat monorepo-split-net.twisterrob.travel.rules -travel"
start "Cinema" cmd /K "run.bat monorepo-split-net.twisterrob.cinema.rules -cinema"
start "Inventory" cmd /K "run.bat monorepo-split-net.twisterrob.inventory.rules -inventory"
start "Others" cmd /K "run.bat monorepo-split-independent_projects.rules -all"
@rem They'll stay open when finished, but the docker container will stop.
