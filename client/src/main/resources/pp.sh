#!/bin/bash

# $1 directory (absolute path)
# $2 file (absolute path)
# $3 model name
# $4 site name
# $5 unixtime

# get the filename without path
FILE=`basename $2`

# format unixtime to human readable
TIME=$(date --date="@$5" +%d.%m.%Y_%H:%M)

# define filename of end result
MP4=$(echo "$1/$4_$3_$TIME.mp4")

# remux ts to mp4
ffmpeg -i $2 -c:v copy -c:a copy -f mp4 $MP4

# move mp4 to target directory
mv $MP4 /tmp

# delete the original .ts file
rm $2

# delete the directory of the recording
rm -r $1
