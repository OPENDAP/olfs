#!/bin/bash
HR="###########################################################################"
###########################################################################
# loggy()
function loggy(){
  echo  "$@" | awk '{ print "# "$0;}'  >&2
}

function check_status() {
  local status=$1
  local cmd=$2
  if [[ $status -eq 0 ]]; then
      loggy "SUCCESS!"
  else
      loggy "FAILURE! status: $status"
      exit $status
  fi
}

loggy "$HR"
loggy
loggy "Updating tracking branch..."
loggy
MAIN_BRANCH="${1:-"master"}"
#MAIN_BRANCH="t1"
loggy "MAIN_BRANCH: '$MAIN_BRANCH'"

TRACKING_BRANCH="${2:-"tomcat-11"}"
#TRACKING_BRANCH="t2"
loggy "TARGET_BRANCH: '$TRACKING_BRANCH'"

loggy "Checking current branch..."
current_branch="$(git branch --show-current)"
check_status $?
loggy "The current branch is: '$current_branch'"
loggy

loggy "Checking out branch: '$TRACKING_BRANCH'"
git checkout "$TRACKING_BRANCH"
check_status $?

loggy "Merging branch '$MAIN_BRANCH' into branch: '$TRACKING_BRANCH'"
git merge --verbose --no-edit "$MAIN_BRANCH"
check_status $?

loggy "Pushing changes for branch: '$TRACKING_BRANCH'"
git push
check_status $?

loggy "The branch '$MAIN_BRANCH' has been merged to the branch '$TRACKING_BRANCH' and the result has been pushed."
