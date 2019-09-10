#! /usr/bin/env perl

######################################################################
# Copyright (c) 2017 Castalia Solutions
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
######################################################################

use strict;
use warnings;

use Mojo::UserAgent;
use Mojo::JSON qw(decode_json encode_json);
use Data::Dumper;

my $base_url;
if ( exists( $ENV{'SCAVA_HOST'} ) ) {
    $base_url = $ENV{'SCAVA_HOST'};
} else {
    $base_url = 'http://ci4.castalia.camp';
    #$base_url = 'http://scava-dev.ow2.org';
}

if ( scalar(@ARGV) != 1 ) {
    print "Usage: $0 project\n";
    print "Example: \n";
    print "    $0 modeling.sirius \n";
    exit;
}

my $project = shift;

my @metric_providers = (
    "trans.rascal.api.changedMethods",
    "trans.rascal.dependency.osgi.numberOSGiBundleDependencies.historic",
    "trans.rascal.dependency.maven.ratioOptionalMavenDependencies",
    "trans.rascal.dependency.maven.numberUniqueMavenDependencies",
    "trans.rascal.dependency.maven.allOptionalMavenDependencies",
    "org.eclipse.scava.metricprovider.historic.bugs.sentiment.SentimentHistoricMetricProvider",
    "org.eclipse.scava.metricprovider.historic.bugs.topics.TopicsHistoricMetricProvider",
    "org.eclipse.scava.metricprovider.historic.configuration.docker.smells",
    "org.eclipse.scava.metricprovider.historic.configuration.docker.dependencies",
    "org.eclipse.scava.metricprovider.historic.newsgroups.emotions.EmotionsHistoricMetricProvider",
    "org.eclipse.scava.metricprovider.historic.bugs.patches.PatchesHistoricMetricProvider",
    "org.eclipse.scava.metricprovider.historic.bugs.severity.SeverityHistoricMetricProvider",
    "org.eclipse.scava.metricprovider.historic.bugs.severitybugstatus.SeverityBugStatusHistoricMetricProvider",
    "org.eclipse.scava.metricprovider.historic.bugs.severityresponsetime.SeverityResponseTimeHistoricMetricProvider",
    "org.eclipse.scava.metricprovider.historic.bugs.severitysentiment.SeveritySentimentHistoricMetricProvider",
    "org.eclipse.scava.metricprovider.historic.bugs.newbugs.NewBugsHistoricMetricProvider",
    "org.eclipse.scava.metricprovider.historic.bugs.comments.CommentsHistoricMetricProvider",
    );

my $url_auth = $base_url . ":8086/api/authentication";
my $url_create_task = $base_url . ":8182/analysis/task/create";
my $url_start_task = $base_url . ":8182/analysis/task/start";;
    
# Prepare for http requests
my $ua  = Mojo::UserAgent->new;
my $auth;

# Authenticate against API
print "\n# Authenticating against ${base_url}.\n\n";
my $tx = $ua->post( 
    $url_auth => { 'Content-Type' => 'application/json'} => json => 
    {
	"username" => "admin",
	"password" => "admin",
    } 
    );

if (not $tx->result->is_success) {
    print "Error: Could not authenticate against API.\n";
    print Dumper($tx->result->error) . "\n";
    exit 8;
}

$auth = $tx->result->headers->authorization;
print "  Authorisation is $auth.\n";

print "# Creating task.\n\n";

my $json_create = {
    "analysisTaskId" => "${project}_task",
    "label" => "${project}_task",
    "projectId" => $project,
    "type" => "SINGLE_EXECUTION", # Could be CONTINUOUS_MONITORING
    "startDate" => "01/01/2018",
    "endDate" => "31/12/2018",
    "metricProviders" => \@metric_providers,
};

print "# Fetching ${url_create_task}.\n\n";
$tx = $ua->post(
    $url_create_task => {
	"Authorization" => $auth, 
	'Content-Type' => 'application/json'
    } => json => $json_create,
    );

if (not $tx->result->is_success) {
    print "Error: Could not get resource $url_create_task.\n";
    print Dumper($tx->result->error) . "\n";
    exit 8;
}

my $data = decode_json($tx->result->body);
my $task_id = $data->{'analysisTaskId'};
print "Created task $task_id.\n\n";

print "Starting task $task_id...\n";

my $json_start = {
    'analysisTaskId' => "$task_id",
};

print "# Fetching ${url_start_task}.\n\n";
$tx = $ua->post(
    $url_start_task => {
	"Authorization" => $auth, 
	'Content-Type' => 'application/json'
    } => json => $json_start,
    );


if (not $tx->result->is_success) {
    print "Error: Could not get resource $url_start_task.\n";
    print Dumper($tx->result->error) . "\n";
    exit 8;
}

