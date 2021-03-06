#!/usr/bin/env perl

############################################################################           
#  Licensed to the Apache Software Foundation (ASF) under one or more                  
#  contributor license agreements.  See the NOTICE file distributed with               
#  this work for additional information regarding copyright ownership.                 
#  The ASF licenses this file to You under the Apache License, Version 2.0             
#  (the "License"); you may not use this file except in compliance with                
#  the License.  You may obtain a copy of the License at                               
#                                                                                      
#      http://www.apache.org/licenses/LICENSE-2.0                                      
#                                                                                      
#  Unless required by applicable law or agreed to in writing, software                 
#  distributed under the License is distributed on an "AS IS" BASIS,                   
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.            
#  See the License for the specific language governing permissions and                 
#  limitations under the License.                                                      
                                                                                       


###########################################################################
# Package: test_harness
#
# This is the top level bootstraping script for the test harness, 
#
#
#
# SYNOPSIS:
#
# test_harness  -help | -c <cluster> |  -h <dir> | (-testjar <jar> -testconfigpath <path>) [-r <retention_days>] [-latest yes] [-x local] [-cleanuponly] [-secretDebugCmd] [-t <testcase>] <configfile>
# test_harness [OPTIONS] conffile [... confile]
#
# - OPTIONS:
# -conf <harness config file> - set name of global harness config file
# -l <log file name> - set log file name
# -t <test group name> - set test group testcases
# -d <description name> - set description for MySQL database
# -r <regexp> - set regular expression for test group testcases
# -db <0 or 1> - disable using MySQL database if set to 0
# -st <group name> - start test from provided group name
#
#
#  Dependencies:
#
#  The main program relies on three configuration/properties files
#  - deploy.properties
#  - test.properties
#  - test_harness/test_harness.conf
#
#  It will look for the properties file under: $ROOT/conf
#
#
#  Returns:
# 
#  0 if no failures and no errors,
#  the sum of failures and errors otherwise



# TODO
# 1. Add -regex option.

use strict;
use File::Path;
use Getopt::Long;
use Cwd;



#  Var: $ROOT
#  The root directory for the harness.
#
#  The main pogram relies on the top level directory $ROOT to be set
#  to the root directory of the harness, it sets it as follows:
#


our $ROOT = (defined($ENV{'HARNESS_ROOT'}) ? $ENV{'HARNESS_ROOT'} :
  die "FATAL ERROR: $0 - You must set HARNESS_ROOT to the root directory of the harness");

unshift( @INC, "$ROOT/libexec" );
unshift( @INC, ".");

require TestDriver;
require TestDriverFactory;
require TestDeployer;
require TestDeployerFactory;
require Insert2Mysql;
require Properties;
require Log; # why aren't we using log4perl?

# Var: $dblog
# The database log. This is set in config file.
our $dblog; 

##############################################################################
#  Sub: usage
#  Print usage statement
#
# Returns:
#  usage string
sub usage
{
    return 
"Usage: $0 [OPTIONS] conffile [... confile]
    OPTIONS:
    -l <log file name> - set log file name
    -t <test group name> - set test group testcases
    -d <description name> - set description for MySQL database
    -r <regexp> - set regular expression for test group testcases
    -db <0 or 1> - disable using MySQL database if set to 0
    -st <group name> - start test from provided group name
    -deploycfg <deploy cfg file> -deploy - Deploy the test setup before testing
        <deploy cfg file> is the configuration file for deployment
    -deploycfg <deploy cfg file> -undeploy - Undeploy the test setup after testing
        <deploy cfg file> is the configuration file for deployment
    ";
}

##############################################################################
#  Sub: readCfg
# Read the configuration file.  The config file is in Perl format so we'll
# just eval it.  If anything goes wrong we'll complain and quit.
#
# Var: cfgFile
# Full path name of config file
#
# Returns:
# returns reference to hash built from cfg file.
#

sub readCfg($)
{
	my $cfgFile = shift;

	open CFG, "< $cfgFile" or die "FATAL ERROR $0 at ".__LINE__.":  Can't open $cfgFile, $!\n";

	my $cfgContents;

	$cfgContents .= $_ while (<CFG>);

	close CFG;

	my $cfg = undef;
        eval("$cfgContents");
	#my $cfg = eval("$cfgContents");

	if ($@) {
		chomp $@;
		die "FATAL ERROR $0 at ".__LINE__." : Error reading config file <$cfgFile>, <$@>\n";
	}

	if (not defined $cfg) {
		die "FATAL ERROR $0 at ".__LINE__." : Configuration file <$cfgFile> should have defined \$cfg\n";
	}

	# Add the name of the file
	$cfg->{'file'} = $cfgFile;

	return $cfg;
}



##############################################################################
# Sub: 
#
# Var: %testStatuses
# A hash containing the resutls from each test : pass, fail, error
#
# Returns: int
# returns 0 if no failures and no errors,
# the sum of failures and errors otherwise
#

sub exitStatus
{
    my ($testStatuses) = @_;
    my $subName  = (caller(0))[3];

    my $passedStr  = 'passed';
    my $failedStr  = 'failed';
    my $abortedStr = 'aborted';
    my $dependStr  = 'failed_dependency';
    my $skippedStr = 'skipped';

    my ($pass, $fail, $abort, $depend, $skipped) = (0, 0, 0, 0, 0);

    foreach (keys(%$testStatuses)) {
        ($testStatuses->{$_} eq $passedStr)  && $pass++;
        ($testStatuses->{$_} eq $failedStr)  && $fail++;
        ($testStatuses->{$_} eq $abortedStr) && $abort++;
        ($testStatuses->{$_} eq $dependStr)  && $depend++;
        ($testStatuses->{$_} eq $skippedStr) && $skipped++;
    }

    return ($fail + $abort); 
}


##############################################################################
# Sub: main
# Gets the corresponding test driver and runs the tests.
#
# - Reads the global config file.
# - Reads ARGV as described in "usage"
# - Attaches to the database and gets a test run id.
# - Loads the configuration file.
# - Gets the test driver that will be used to parse test file.
# - Runs the tests by invoking the run command from the test driver.
# - Prints the final results.
#
#
# Var: $logfile
# The test run log name.
# If no logfile is specified , then it is configured as follows  :
# The file name for the test result log. The location of the log directory
# is obtained from the configuration value . The filename
# is stored as $globalCfg{localoutpathbase}/test_harness_log_{time}
#
# Var: $testrun_desc
# A description of the test run to be recorded in the logs.
#
# Var: @testgroups
# A list of all the test groups. This is passed in as a command line option.
#
# Var: @testMatches
# A list of test patterns specified by the  "-t option" . If none was passed then all tests match.
#
# Var: $globalCfg
# All values to be shared globally. The $harnessCfg values are stored in the globalConfig
#
# Var: $harnessCfg
#  The configuration file. It assumes it is located at
# $ROOT/conf/test_harness/test_harness.conf. 
#
# Var: $log
# The test log.
#
# Var: $dbh
# Instance of Insert2Mysal, this object provides database access subroutines.
#
# Var: %testStatuses
# A hash containing the resutls from each test : pass, fail, error
 

my $logfile = "";
my $testrun_desc = 'none';
my @testgroups;
my @testMatches;
my $startat = undef;
my $deploycfg = undef;
my $deploy = undef;
my $undeploy = undef;
my $help=0;

die usage() if (@ARGV == 0);

# Arguments on the command line can override values in the conf file
# so first read in the conf file, then process the arguments.
# But conf file can be specified on command line, so 
# get that from ARGV before processing other arguments.

# Find the harness config file
# 1) Use command line option -conf if given
# 2) else use env var PIG_HARNESS_CONF if set
# 3) else use default.conf if found
# 4) else fail

my $harnessCfg = "";
for (my $i = 0; $i < @ARGV; $i++) {
  if ($ARGV[$i] eq "-conf") {
    $harnessCfg = $ARGV[$i + 1];
    splice(@ARGV, $i, 2);
    last;
  }
}
if ($harnessCfg eq "") {
  if (defined($ENV{'HARNESS_CONF'})) {
    $harnessCfg = $ENV{'HARNESS_CONF'};
  } else {
    $harnessCfg = "$ROOT/conf/default.conf";
  }
}

# Read the global config file
my $globalCfg = "";
if ( -e "$harnessCfg" ) {
   $globalCfg = readCfg("$harnessCfg");
   $globalCfg->{'harnessCfg'} = $harnessCfg;
   
} else {
   die "FATAL ERROR: $0 at ".__LINE__." - Configuration file <$harnessCfg> does NOT exist\n";
}

# *pig*  -help | -c <cluster> |  -h <dir> | (-testjar <jar> -testconfigpath <path>) [-r <retention_days>] [-latest yes] [-x local] [-cleanuponly] [-secretDebugCmd] [-t <testcase>] <configfile>


while ($ARGV[0] =~ /^-/) {
               #print $log "DEBUG $0 : ARGV(0)= ".$ARGV[0]."\n";
	if ($ARGV[0] =~ /^--?l(og)?$/) {
		shift;
		$logfile = shift;
		next;
	}

	if ($ARGV[0] =~ /^--?t(estgroup)?$/) {
		shift;
		push @testgroups, shift;
               #print $log "DEBUG $0 : TESTGROUP ".$ARGV[0]."\n";
		next;
	}

	if ($ARGV[0] =~ /^--?d(escription)?$/) {
		shift;
		$testrun_desc = shift;
		next;
	}

	if ($ARGV[0] =~ /^--?r(egexp)?$/) {
		shift;
		push @testMatches, shift;
		next;
	}
	if ($ARGV[0] =~ /^--?db(log)?$/) {
		shift;
		$dblog = shift;
		next;
	}

	if ($ARGV[0] =~ /^--?st(artat)?$/) {
		shift;
		$startat = shift;
		next;
	}

	if ($ARGV[0] =~ /^--?deploycfg$/) {
		shift;
		$deploycfg = shift;
		next;
	}

	if ($ARGV[0] =~ /^--?deploy$/) {
		shift;
		$deploy = 1;
		next;
	}

	if ($ARGV[0] =~ /^--?undeploy$/) {
		shift;
		$undeploy = 1;
		next;
	}

	# Not an argument for us, so just push it into the hash.  These arguments
	# will override values in the config file.
	my $key = shift;
	$key =~ s/^--?//;
	$globalCfg->{$key} = shift;
}

mkpath( [  $globalCfg->{'localoutpathbase'} ] , 1, 0777) if ( ! -e  $globalCfg->{'localoutpathbase'} );
$globalCfg->{'UID'}= time;
$logfile = $globalCfg->{'localoutpathbase'} . "/test_harnesss_" . $globalCfg->{'UID'} if $logfile eq "";
$globalCfg->{'logfile'} = $logfile;


my $log;
open $log, "> $logfile" or die "FATAL ERROR $0 at ".__LINE__." : Can't open $logfile, $!\n";

print "================================================================================================\n";
print "LOGGING RESULTS TO " . Cwd::realpath($logfile) . "\n";
print "================================================================================================\n";

# If they have requested deployment, do it now
if ($deploy) {
    if (!$deploycfg) {
        die "You must define a deployment configuration file using -deploycfg "
            . "<cfg file> if you want to deploy your test resources.\n";
    }

    # Read the deployment cfg file
    print $log "INFO: $0 at ".__LINE__." : Loading configuration file $deploycfg\n";
    my $cfg = readCfg($deploycfg);

	# Copy the global config into our cfg
	foreach(keys(%$globalCfg)) {
		next if $_ eq 'file';
		$cfg->{$_} = $globalCfg->{$_}; #foreach(keys(%$globalCfg));
	}

    # Instantiate the TestDeployer
    my $deployer = TestDeployerFactory::getTestDeployer($cfg);
    die "FATAL: $0: Deployer does not exist\n" if ( !$deployer );

    eval {
        $deployer->checkPrerequisites($cfg, $log);
    };
    if ($@) {
        chomp $@;
        die "Check of prerequites failed: <$@>\n";
    }
    eval {
        $deployer->deploy($cfg, $log);
    };
    if ($@) {
        chomp $@;
        die "Deployment of test resources failed: <$@>\n";
    }
    eval {
        $deployer->start($cfg, $log);
    };
    if ($@) {
        chomp $@;
        die "Failed to start test resources: <$@>\n";
    }
    eval {
        $deployer->generateData($cfg, $log);
    };
    if ($@) {
        chomp $@;
        die "Failed to generate data for testing: <$@>\n";
    }
    eval {
        $deployer->confirmDeployment($cfg, $log);
    };
    if ($@) {
        chomp $@;
        die "Failed to confirm that test resources were properly deployed: <$@>\n";
    }

    print $log "INFO: $0 at " . __LINE__ .
        " : Successfully deployed test resources $deploycfg\n";
}

# If they said -undeploy test up front that they have a deploycfg file and that we
# can read it so we lower the risk of running all the tests and then failing to
# undeploy.
if ($undeploy) {
    if (!$deploycfg) {
        die "You must define a deployment configuration file using -deploycfg "
            . "<cfg file> if you want to undeploy your test resources.\n";
    }

    # Read the deployment cfg file
    print $log "INFO: $0 at ".__LINE__." : Loading configuration file $deploycfg\n";
    my $cfg = readCfg($deploycfg);

    # Instantiate the TestDeployer
    my $deployer = TestDeployerFactory::getTestDeployer($cfg);
    die "FATAL: $0: Deployer does not exist\n" if ( !$deployer );
}


print $log "Beginning test run at " . time . "\n";

my $dbh = undef;
if($dblog) {
	# Attach to the database
	$dbh = new Insert2Mysql($globalCfg->{'dbServer'}, $globalCfg->{'dbDatabase'});
	$globalCfg->{'trid'} = $dbh->startTestRun($testrun_desc);
	$dbh->logTestRun($globalCfg->{'trid'}, $logfile);

	# print "Testrun id in database is $globalCfg->{'trid'}\n";
	print $log "Testrun id  $globalCfg->{'trid'}\n";
}


my %testStatuses;
foreach my $arg (@ARGV) {
    print $log "INFO: $0 at ".__LINE__." : Loading configuration file $arg\n";
	my $cfg = readCfg($arg);
	# Copy contents of global config file into hash.
	foreach(keys(%$globalCfg)) {
		next if $_ eq 'file';
		$cfg->{$_} = $globalCfg->{$_}; # foreach(keys(%$globalCfg));
		print $log "\nINFO $0: $_=".$cfg->{$_};
	}
	print $log "\n"; 

	my $driver = TestDriverFactory::getTestDriver($cfg);
        die "FATAL: $0: Driver does not exist\n" if ( !$driver );
	$driver->run(\@testgroups, \@testMatches, $cfg, $log, $dbh, \%testStatuses, $arg, $startat, $logfile);
}
$dbh->endTestRun($globalCfg->{'trid'}) if ($dblog);

# don't remove the space after Final results, it matters.
TestDriver::printResults(\%testStatuses, $log, "Final results ");
print $log  "Finished test run at " . time . "\n";

# If they have requested undeployment, do it now
if ($undeploy) {
    # Read the deployment cfg file
    print $log "INFO: $0 at ".__LINE__." : Loading configuration file $deploycfg\n";
    my $cfg = readCfg($deploycfg);

    # Instantiate the TestDeployer
    my $deployer = TestDeployerFactory::getTestDeployer($cfg);
    die "FATAL: $0: Deployer does not exist\n" if ( !$deployer );

    eval {
        $deployer->deleteData($cfg, $log);
    };
    if ($@) {
        chomp $@;
        warn "Failed to delete data as part of undeploy: <$@>\n";
    }
    eval {
        $deployer->stop($cfg, $log);
    };
    if ($@) {
        chomp $@;
        warn "Failed to stop test resources: <$@>\n";
    }
    eval {
        $deployer->undeploy($cfg, $log);
    };
    if ($@) {
        chomp $@;
        warn "Failed to undeploy test resources: <$@>\n";
    }
    eval {
        $deployer->confirmUndeployment($cfg, $log);
    };
    if ($@) {
        chomp $@;
        die "Failed to confirm that test resources were properly undeployed: <$@>\n";
    }

    print $log "INFO: $0 at " . __LINE__ .
        " : Successfully undeployed test resources $deploycfg\n";
}
close $log;

exit exitStatus(\%testStatuses);

