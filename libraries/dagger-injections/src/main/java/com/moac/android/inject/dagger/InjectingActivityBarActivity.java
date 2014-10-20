/*
* Copyright (c) 2013 Fizz Buzz LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

/*
* Copyright (c) 2013 Fizz Buzz LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.moac.android.inject.dagger;

import android.app.Activity;
import android.support.v7.app.ActionBarActivity;

import java.util.ArrayList;
import java.util.List;

import dagger.ObjectGraph;

public class InjectingActivityBarActivity extends ActionBarActivity implements Injector {

    private ObjectGraph mObjectGraph;

    /**
     * Gets this Activity's object graph.
     *
     * @return the object graph
     */
    @Override
    public final ObjectGraph getObjectGraph() {
        return mObjectGraph;
    }

    /**
     * Injects a target object using this Activity's object graph.
     *
     * @param target the target object
     */
    @Override
    public void inject(Object target) {
        mObjectGraph.inject(target);
    }

    // implement Injector interface

    /**
     * Creates an object graph for this Activity by extending the application-scope object graph with the modules
     * returned by {@link #getModules()}.
     * <p/>
     * Injects this Activity using the created graph.
     */
    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // extend the application-scope object graph with the modules for this activity
        mObjectGraph = ((Injector) getApplication()).getObjectGraph().plus(getModules().toArray());

        // now we can inject ourselves
        inject(this);
    }

    @Override
    protected void onDestroy() {
        // Eagerly clear the reference to the activity graph to allow it to be garbage collected as
        // soon as possible.
        mObjectGraph = null;
        super.onDestroy();
    }

    /**
     * Returns the list of dagger modules to be included in this Activity's object graph. Subclasses that override
     * this method should add to the list returned by super.getModules().
     *
     * @return the list of modules
     */
    protected List<Object> getModules() {
        return new ArrayList<>();
    }
}
