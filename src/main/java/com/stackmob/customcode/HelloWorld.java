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

import com.stackmob.core.DatastoreException;
import com.stackmob.core.InvalidSchemaException;
import com.stackmob.core.customcode.CustomCodeMethod;
import com.stackmob.core.rest.ProcessedAPIRequest;
import com.stackmob.core.rest.ResponseToProcess;
import com.stackmob.sdkapi.DataService;
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

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HelloWorld implements CustomCodeMethod {

  @Override
  public String getMethodName() {
    return "submit_question";
  }

  @Override
  public List<String> getParams() {
	  return new ArrayList<String>();
  }

  @Override
  public ResponseToProcess execute(ProcessedAPIRequest request, SDKServiceProvider serviceProvider) {
	  String loggedInUser = request.getLoggedInUser();
	  if(loggedInUser == null) {
		  return new ResponseToProcess(HttpURLConnection.HTTP_FORBIDDEN);
	  }
	  
	  DataService ds = serviceProvider.getDataService();
	  
	  String questionId = request.getParams().get("question_id");
	  String answer = request.getParams().get("answer");
	  
	    Map<String, Object> response = new HashMap<String, Object>();
	 
	    try {
	           
	    	List<SMCondition> query = new ArrayList<SMCondition>();
	    	query.add(new SMEquals("question_id", new SMString(questionId)));
	 
	        List<SMObject> questions = ds.readObjects("question", query);
	        SMObject question = questions.get(0);
	        
	        SMValue<String> correctAnswer = question.getValue().get("correct_answer");
	        String correctAnswerString = correctAnswer.getValue();
	        
	        boolean correctlyAnswered = answer.equals(correctAnswer);
	        int userPointsIncrement;
	        if(correctlyAnswered) {
	        	Boolean hasBeenCorrectlyAnsweredPreviously = (Boolean) question.getValue().get("correctly_answered").getValue();
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
	        
	        long newUserPointsTotal = incrementUserPoints(ds, loggedInUser, userPointsIncrement);
	        
	        response.put("correct", correctlyAnswered);
	        response.put("points", newUserPointsTotal);
	 
	    } catch (InvalidSchemaException ise) {
	    	response.put("message", ise.getMessage());
	    	return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, response);
	    } catch (DatastoreException dse) {
	    	response.put("message", dse.getMessage());
	    	return new ResponseToProcess(HttpURLConnection.HTTP_INTERNAL_ERROR, response);
	    }

    return new ResponseToProcess(HttpURLConnection.HTTP_OK, response);
  }
  
	private void markQuestionAsCorrectlyAnswered(DataService ds, String questionId) throws InvalidSchemaException, DatastoreException {
	  List<SMUpdate> update = new ArrayList<SMUpdate>();
	  update.add(new SMSet("correctly_answered", new SMBoolean(true)));
	  ds.updateObject("question", new SMString(questionId), update);
	  }
	
	private long incrementUserPoints(DataService ds, String username, int userPointsIncrement) throws InvalidSchemaException, DatastoreException {
		List<SMCondition> query = new ArrayList<SMCondition>();
		query.add(new SMEquals("user", new SMString(username)));
		List<SMObject> users = ds.readObjects("user", query);
	    SMObject user = users.get(0);
	    
	    SMValue<Integer> userPoints = user.getValue().get("points");
	    Integer currentPointsTotal = userPoints.getValue();
	    
	    long newPointsTotal;
	    if(userPointsIncrement == 0) {
	    	newPointsTotal = currentPointsTotal;
	    }
	    else {
	    	newPointsTotal = currentPointsTotal + userPointsIncrement;
	        List<SMUpdate> update = new ArrayList<SMUpdate>();
	        update.add(new SMSet("points", new SMInt(newPointsTotal)));
	        ds.updateObject("user", new SMString(username), update);
	    }
	
	    return newPointsTotal;
	}

}
