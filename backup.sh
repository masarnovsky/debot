#!/bin/bash

# chmod u+x backup.sh to make file executable

echo "backup database"
docker exec -t postgresdb pg_dumpall -c -U debot > backup/debot/debot_dump_`date +%d-%m-%Y"_"%H_%M_%S`.sql
echo "backup finished"