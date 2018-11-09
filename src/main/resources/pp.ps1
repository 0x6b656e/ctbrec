# parse command line parameters
param (
	[Parameter(Mandatory=$true)][string]$dir,
	[Parameter(Mandatory=$true)][string]$file,
	[Parameter(Mandatory=$true)][string]$model,
	[Parameter(Mandatory=$true)][string]$site,
	[Parameter(Mandatory=$true)][string]$time
)

# convert unixtime into a date object
$epoch = get-date "1/1/1970"
$date = $epoch.AddSeconds($time)

# print out a theoretical new file name, you could use "rename" here, to rename the file
# or move it somewhere or ...
$newname = "$($model)_$($site)_$($date.toString("yyyyMMdd-HHmm")).ts"
ren $file $newname