#!/bin/bash
HR="-- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --"
###########################################################################
# loggy()
#
function loggy(){
  echo  "$@" | awk '{ print "# "$0;}'  >&2
}

###########################################################################
# check_status()
#
function check_status() {
  local status=$1
  local cmd="$2"
  if [[ $status -eq 0 ]]; then
      loggy "SUCCESS!"
  else
      loggy "FAILURE! The '$cmd' command exited with status: $status"
      exit $status
  fi
}

###########################################################################
# main()
#

loggy "$HR"
loggy
loggy "Updating tracking branch..."
loggy

git config --global user.name "The-Robot-Travis"
git config --global user.email "npotter@opendap.org"

MAIN_BRANCH="${1:-"master"}"
#MAIN_BRANCH="t1"
loggy "MAIN_BRANCH: '$MAIN_BRANCH'"

TRACKING_BRANCH="${2:-"tomcat-11"}"
#TRACKING_BRANCH="t2"
loggy "TARGET_BRANCH: '$TRACKING_BRANCH'"

loggy "Checking out origin/$TRACKING_BRANCH"
git checkout origin "$TRACKING_BRANCH"
check_status $? "git checkout origin $TRACKING_BRANCH"

loggy "Listing Branches..."
git branch -a -r
check_status $? "git branch -a -r"

loggy "Checking out branch: '$MAIN_BRANCH'"
git checkout "$MAIN_BRANCH"
check_status $? "git checkout $MAIN_BRANCH"

loggy "Checking current branch..."
current_branch="$(git branch --show-current)"
check_status $? "git branch --show-current"
loggy "The current branch is now: '$current_branch'"
loggy

loggy "Checking out branch: '$TRACKING_BRANCH'"
git checkout "$TRACKING_BRANCH"
check_status $? "git checkout $TRACKING_BRANCH"

loggy "Merging branch '$MAIN_BRANCH' into branch: '$TRACKING_BRANCH'"
git merge --verbose --no-edit "$MAIN_BRANCH"
check_status $? "git merge --verbose --no-edit $MAIN_BRANCH"

loggy "Pushing changes for branch: '$TRACKING_BRANCH'"
git push
check_status $? "git push"

loggy "The branch '$MAIN_BRANCH' has been merged to the branch '$TRACKING_BRANCH' and the result has been pushed."

loggy
loggy "$HR"
