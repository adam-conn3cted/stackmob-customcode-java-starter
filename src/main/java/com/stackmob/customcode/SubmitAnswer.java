/**
 * Copyright 2012-2013 StackMob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stackmob.customcode;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stackmob.core.DatastoreException;
import com.stackmob.core.InvalidSchemaException;
import com.stackmob.core.customcode.CustomCodeMethod;
import com.stackmob.core.rest.ProcessedAPIRequest;
import com.stackmob.core.rest.ResponseToProcess;
import com.stackmob.sdkapi.DataService;
import com.stackmob.sdkapi.LoggerService;
import com.stackmob.sdkapi.SDKServiceProvider;
import com.stackmob.sdkapi.SMBoolean;
import com.stackmob.sdkapi.SMCondition;
import com.stackmob.sdkapi.SMEquals;
import com.stackmob.sdkapi.SMInt;
import com.stackmob.sdkapi.SMObject;
import com.stackmob.sdkapi.SMSet;
import com.stackmob.sdkapi.SMString;
import com.stackmob.sdkapi.SMUpdate;
import com.stackmob.sdkapi.SMValue;

public class SubmitAnswer implements CustomCodeMethod {

  @Override
  public String getMethodName() {
    return "submit_answer";
  }

  @Override
  public List<String> getParams() {
	  return new ArrayList<String>();
  }

  @Override
  public ResponseToProcess execute(ProcessedAPIRequest request, SDKServiceProvider serviceProvider) {
	  LoggerService logger = serviceProvider.getLoggerService(SubmitAnswer.class);
	  
	  String loggedInUser = request.getLoggedInUser();
	  
	  logger.debug("Call to submit_question with user '" + loggedInUser + "'");
	  
	  loggedInUser = "jacque";
	  if(loggedInUser == null) {
		  return new ResponseToProcess(HttpURLConnection.HTTP_FORBIDDEN);
	  }
	  
	  DataService ds = serviceProvider.getDataService();
	  
	  String questionId = request.getParams().get("question_id");
	  String answer = request.getParams().get("answer");
	  
	  logger.debug("Call to submit_question with questionId '" + questionId + "'");
	  logger.debug("Call to submit_question with answer '" + answer + "'");
	  
	    Map<String, Object> response = new HashMap<String, Object>();
	 
	    try {
	           
	    	List<SMCondition> query = new ArrayList<SMCondition>();
	    	query.add(new SMEquals("question_id", new SMString(questionId)));
	 
	        List<SMObject> questions = ds.readObjects("question", query);
	        SMObject question = questions.get(0);
	        
	        SMValue<String> correctAnswer = question.getValue().get("correct_answer");
	        String correctAnswerString = correctAnswer.getValue();
	        
	        logger.debug("Correct answer to question is '" + correctAnswerString + "'");
	        
	        boolean correctlyAnswered = answer.equals(correctAnswerString);
	        
	        logger.debug("User's answer was " + (correctlyAnswered ? "correct" : "incorrect"));
	        
	        int userPointsIncrement;
	        if(correctlyAnswered) {
	        	SMValue<Boolean> correctlyAnsweredValue = question.getValue().get("correctly_answered");
	        	Boolean hasBeenCorrectlyAnsweredPreviously = correctlyAnsweredValue == null ? false : correctlyAnsweredValue.getValue();
	        	
	        	logger.debug("Question has " + (hasBeenCorrectlyAnsweredPreviously ? "" : "not") + " been answered correctly previously");
	        	
	        	if(hasBeenCorrectlyAnsweredPreviously) {
	        		userPointsIncrement = 1;
	        	}
	        	else {
	        		userPointsIncrement = 5;
	        		markQuestionAsCorrectlyAnswered(ds, questionId);
	        	}
	        }
	        else {
	        	userPointsIncrement = 0;
	        }
	        
	        long newUserPointsTotal = incrementUserPoints(ds, loggedInUser, userPointsIncrement, logger);
	        
	        logger.debug("User's new points total is '" + newUserPointsTotal + "'");
	        
	        response.put("correct", correctlyAnswered);
	        response.put("points", newUserPointsTotal);
	 
	    } catch (InvalidSchemaException ise) {
	    	logger.error("Unable to process submit question request", ise);
	    	response.put("message", ise.getMessage());
	    	return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, response);
	    } catch (DatastoreException dse) {
	    	logger.error("Unable to process submit question request", dse);
	    	response.put("message", dse.getMessage());
	    	return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, response);
	    }
	    
	    logger.debug("Answer submitted successfully with response:\n" + response);

    return new ResponseToProcess(HttpURLConnection.HTTP_OK, response);
  }
  
	private void markQuestionAsCorrectlyAnswered(DataService ds, String questionId) throws InvalidSchemaException, DatastoreException {
	  List<SMUpdate> update = new ArrayList<SMUpdate>();
	  update.add(new SMSet("correctly_answered", new SMBoolean(true)));
	  ds.updateObject("question", new SMString(questionId), update);
	  }
	
	private long incrementUserPoints(DataService ds, String username, int userPointsIncrement, LoggerService logger) throws InvalidSchemaException, DatastoreException {
		List<SMCondition> query = new ArrayList<SMCondition>();
		query.add(new SMEquals("user", new SMString(username)));
		List<SMObject> users = ds.readObjects("user", query);
		
		logger.debug("In incrementUserPoints(), found " + users.size() + " users");
		
	    SMObject user = users.get(0);
	    
	    logger.debug("user found: " + user);
	    
	    SMValue<Integer> userPoints = user.getValue().get("points");
	    Integer currentPointsTotal = userPoints.getValue();
	    
	    logger.debug("User's current points total: '" + currentPointsTotal + "'");
	    
	    long newPointsTotal;
	    if(userPointsIncrement == 0) {
	    	newPointsTotal = currentPointsTotal;
	    }
	    else {
	    	newPointsTotal = currentPointsTotal + userPointsIncrement;
	    	
	    	logger.debug("Setting points total to '" + newPointsTotal + "' for user '" + username + "'");
	    	
	        List<SMUpdate> update = new ArrayList<SMUpdate>();
	        update.add(new SMSet("points", new SMInt(newPointsTotal)));
	        ds.updateObject("user", new SMString(username), update);
	    }
	
	    return newPointsTotal;
	}

}
