package runtime;

public class Runner {

    // Wait on test-request (loop)
    // Validate request
    // Write request to couchdb - key "test-request-" + guid
    // Remove cats directory
    // Build Script with Shell Commands:
    //  run bash
    //  cd to cats directory
    //  git clone cats repo
    //  git checkout relevant branch
    //  Build cats repo
    //  Override properties with any from test-request payload
    //  Execute tests
    // Execute script
    // Collect results json file
    // Write results json file to couchdb key "test-result-json-" + guid
    // Construct json for result
    // Write result json to couchdb key "test-result-" + guid
    // Write result json to test-result topic
}
