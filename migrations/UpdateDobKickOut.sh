#!/bin/bash

echo ""
echo "Applying migration UpdateDobKickOut"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /updateDobKickOut                       controllers.UpdateDobKickOutController.onPageLoad()" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "updateDobKickOut.title = updateDobKickOut" >> ../conf/messages.en
echo "updateDobKickOut.heading = updateDobKickOut" >> ../conf/messages.en

echo "Migration UpdateDobKickOut completed"
