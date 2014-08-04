# humperdink

Tracking API, inspired by The Princess Bride -- it can track a falcon on a cloudy day!

## Pre-reqs for developers

1. Install leiningen (version 2 or higher)
2. Create the file ~/.lein/init.clj with the contents as follows:

    (def lein-beanstalk-credentials
      {:access-key "YOUR-ACCESS-KEY"
       :secret-key "YOUR-SECRET-KEY"})

Now you can deploy with `lein beanstalk deploy devel`. This will
create the elastic beanstalk app if it doesn't exist, or update it if
it does.


## Modes of development

1. Local development mode

`lein ring server`

Test with curl: `curl --data '{"a" [1,2,3], "b" 42}' http://localhost:3000/foo --header "Content-Type:application/json"`

2. AWS Elastic Beanstalk

`lein beanstalk deploy devel` (or stage, or prod)

## Overview

### PART 1: Webserver for writing arbitrary strings to arbitrary filepaths

In a terminal, cd to the project dir and run `lein ring
server-headless`. That should start this webserver on your dev env.

Now you can POST to localhost:3000 with ANY arbitrary URL under
`/log`, and it will write your HTTP body as a line in a logfile with
relative path corresponding to the URL.

E.g. You can POST the body `{"key": "this will appear as a log line in
a file somewhere"}` to the URL
`http://localhost:3000/log/arbitrary/path/begins/here`, and the entire
JSON string will get logged to the file
`./logs/arbitrary/path/begins/here.log`.

These logfiles will be rotated out hourly (current code state is
minutely for dev purposes).



NB: Make sure you use content-type header value of `application/json`
or `text/plain`.

### PART 2: Automatically uploading rotated logfiles to Amazon S3

Spin up a repl and run `upload-rotated-files/-main` method. This
will recursively search the logs directory for files that have been
rotated out, compress them with gzip, upload them to Amazon S3, and
(if delete mode is enabled) delete them from the localhost.

In practice, this would be cronned to run hourly.


## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
