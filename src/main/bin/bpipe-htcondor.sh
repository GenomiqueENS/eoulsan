#!/bin/bash
# vim: expandtab:ts=4

# Start, stop and get status of jobs running on a HTCondor job scheduler.
#
# Usage:
#
# Starting a job (will print job ID on standard output):
#
#    COMMAND="foobar" NAME=test WALLTIME="00:01:00" PROCS=1 QUEUE=main JOBTYPE=single ./bpipe-htcondor.sh start
#
# Stopping a job (given some job id "my_job_id")
#
#    ./bpipe-htcondor.sh stop my_job_id
#
# Getting the status of a job (given some job id "my_job_id")
#
#    ./bpipe-htcondor.sh status my_job_id
#
# Notes:
#
# None of the commands are guaranteed to succeed. An exit status of 0 for this script
# indicates success, all other exit codes indicate failure (see below).
#
# Stopping a job may not cause it to stop immediately. You are advised to check the
# status of a job after asking it to stop. You may need to poll this value.
#
# We are not guaranteed to know the exit status of a job command, for example if the
# job was killed before the command was run.
#
# Inspired by bpipe-slurm.sh from: Bernie Pope, Simon Sadedin, Alicia Oshlack
# Author: Laurent Jourdren
# Copyright 2015.

# This is what we call the program in user messages
program_name=bpipe-htcondor

# exit codes:
SUCCESS=0
INCORRECT_FIRST_ARGUMENT=1 # must be start, stop, or status
MISSING_JOB_PARAMETER=2    # one of the env vars not defined
STOP_MISSING_JOBID=3       # stop command not given job id as parameter
STATUS_MISSING_JOBID=4     # status command not given job id as parameter
CONDOR_RM_FAILED=5              # scancel command returned non-zero exit status
CONDOR_Q_FAILED=6             # scontrol command returned non-zero exit status
CONDOR_QSUB_FAILED=7              # sbatch command returned non-zero exit status
MKDIR_JOBDIR_FAILED=8
JOBTYPE_FAILED=9              # jobtype variable led to non-zero exit status

ESSENTIAL_ENV_VARS="COMMAND NAME"
OPTIONAL_ENV_VARS="WALLTIME PROCS QUEUE JOBDIR JOBTYPE MEMORY"
DEFAULT_BATCH_MEM=4096
DEFAULT_BATCH_PROCS=1
DEFAULT_WALLTIME="01:00:00" # one hour
DEFAULT_QUEUE=debug	#Queue is parition in slurm, will use this with -p
DEFAULT_JOBTYPE=single	#Should be single, smp or mpi


# TODO Use JOBDIR to define where put htcondor description file

# Print a usage message
usage () {
   echo "usage: $program_name (start | stop ID | status ID)"
   echo "start needs these environment variables: $ESSENTIAL_ENV_VARS"
   echo "start will use these variables if defined: $OPTIONAL_ENV_VARS"
}



# Generate a HTCondor description file from parameters found in environment variables.
make_htcondor_description_file () {
   # check that all the essential environment variables are defined
   for v in $ESSENTIAL_ENV_VARS; do
      eval "k=\$$v"
      if [[ -z $k ]]; then
         echo "$program_name ERROR: environment variable $v not defined"
         echo "these environment variables are required: $ESSENTIAL_ENV_VARS"
         exit $MISSING_JOB_PARAMETER
      fi
   done

   # set the walltime
   if [[ -z $WALLTIME ]]; then
      WALLTIME=$DEFAULT_WALLTIME
   fi

   # set the queue
   if [[ -z $QUEUE ]]; then
      QUEUE=$DEFAULT_QUEUE
   fi

  # set the jobtype
   if [[ -z $JOBTYPE ]]; then
      JOBTYPE=$DEFAULT_JOBTYPE
   fi

   if [[ -z "$EOULSAN_TASK_ID" ]];  then
      JOB_SCRIPT_ID=$BASHPID
   else
      JOB_SCRIPT_ID=$EOULSAN_TASK_ID
   fi

   if [[ -z $MEMORY ]]; then
      memory_request=""
   else
      memory_request="request_memory = ${MEMORY}G"
   fi

   if [[ -z $PROCS ]]; then
      procs_request=""
   else
      procs_request="request_cpus = ${PROCS}"
   fi

   if [[ -z $CONCURRENCY_LIMITS ]]; then
      concurrency_limits=""
   else
      concurrency_limits="concurrency_limits = ${CONCURRENCY_LIMITS}"
   fi

   if [[ -z $NICE_USER ]]; then
      nice_user="False"
   else
      nice_user=${NICE_USER}
   fi

   # set the job directory if needed
   if [[ -n $JOBDIR ]]; then
      # check if the directory already exists
      if [[ ! -d "$JOBDIR" ]]; then
         # try to make the directory
         mkdir "$JOBDIR"
         # check if the mkdir succeeded
         if [[ $? != 0 ]]; then
            echo "$program_name ERROR: could not create job directory $JOBDIR"
            exit $MKDIR_JOBDIR_FAILED
         fi
      fi
      job_script_name="$JOBDIR/job-$JOB_SCRIPT_ID.htcondor"
   else
      job_script_name="job-$JOB_SCRIPT_ID.htcondor"
   fi

   job_script_dir=`dirname $job_script_name`

   # write out the job script to a file
   # Output masking unreliable at moment, stores the sbatch stdout and stderr in logs
   EXECUTABLE=`echo $COMMAND | cut -f 1 -d ' '`
   ARGUMENTS=`echo $COMMAND | cut -f 2- -d ' '`
   cat > $job_script_name << HERE
universe = vanilla
$memory_request
$procs_request
executable = $EXECUTABLE
getenv = True
output = $job_script_dir/script-$JOB_SCRIPT_ID.stdout
error = $job_script_dir/script-$JOB_SCRIPT_ID.stderr
arguments = $ARGUMENTS
$concurrency_limits
nice_user = $nice_user
queue
HERE

# TODO Handle InitialDir = <path>/test/run_1

   echo $job_script_name
}




# Launch a job on the queue.
start () {
   # create the job script
   job_script_name=`make_htcondor_description_file`
   # check that the job script file exists
   if [[ -f $job_script_name ]]
      then
         # launch the job and get its id
         job_id_full=`/usr/bin/time -v -o ${job_script_name}.submit.time condor_submit -terse $job_script_name 2> ${job_script_name}.submit.err`
         condor_submit_exit_status=$?
         echo $job_id_full > ${job_script_name}.submit.out

         if [[ $condor_submit_exit_status -eq 0 ]]
            then
               job_id_number=`echo $job_id_full | tr -s ' ' | cut -f 1 -d ' '`
               echo $job_id_number
            else
               echo "$program_name ERROR: sbatch returned non zero exit status $condor_submit_exit_status"
               exit $CONDOR_QSUB_FAILED
         fi
      else
         echo "$program_name ERROR: could not create job script $job_script_name"
   fi
}

# stop a job given its id
# XXX should we check the status of the job first?
stop () {
   # make sure we have a job id on the command line
   if [[ $# -ge 1 ]]
      then
         # try to stop it
         condor_rm "$1"
         condor_rm_success=$?
         if [[ $condor_rm_success == 0 ]]
            then
               exit $SUCCESS
            else
               exit $CONDOR_RM_FAILED
         fi
      else
         echo "$program_name ERROR: stop requires a job identifier"
         exit $STOP_MISSING_JOBID
   fi
}

# get the status of a job given its id
status () {

   # make sure we have a job id on the command line
   if [[ $# -ge 1 ]]
   then
         # get the output of condor_q
         job_state=`condor_q -nobatch -format "%d\n" JobStatus $1`
         condor_q_success=$?

         if [[ $condor_q_success == 0 ]]
         then

               if [[ -z "$job_state"  ]]
               then
                   job_state=`condor_history -limit 1 -format "%d\n" JobStatus $1`
                   condor_history_success=$?

                   if [[ $condor_history_success -ne 0 ]]
                   then
                        exit $CONDOR_Q_FAILED
                   fi
               fi

               case "$job_state" in
                  5|1|H|I) echo WAITING;;
                  2|R) echo RUNNING;;
                  3|X) echo COMPLETE 999;; # Artificial exit code because Slurm does not provide one
                  4|C)
                     command_exit_status=`condor_history -limit 1 -format "%d\n" ExitCode $1 | tr -d ' '`
                     condor_history_success=$?

                     if [[ -z "$command_exit_status" ]]
                     then
                        exit $CONDOR_Q_FAILED
                     fi

                     if [[ $condor_history_success -ne 0 ]]
                     then
                        exit $CONDOR_Q_FAILED
                     fi

                  # it is possible that command_exit_status will be empty
                  # for example we start the job and then it waits in the queue
                  # and then will kill it without it ever running
                  echo "COMPLETE $command_exit_status";;

                  *) echo UNKNOWN;;
               esac
               exit $SUCCESS
         fi

   else
         echo "$program_name ERROR: status requires a job identifier"
         exit $STATUS_MISSING_JOBID
   fi
}

# run the whole thing
main () {
   # check that we have at least one command
   if [[ $# -ge 1 ]]
      then
         case "$1" in
            start)  start;;
            stop)   shift
                      stop "$@";;
            status) shift
                      status "$@";;
            *) usage
               exit $INCORRECT_FIRST_ARGUMENT
            ;;
         esac
      else
         usage
         exit $INCORRECT_FIRST_ARGUMENT
   fi
   exit $SUCCESS
}

main "$@"
