comp512-project-RMI
===============

Each of the client, middleware, and servers can be run on different machines.
The client talks directly to the middleware (makes requests on the middleware's Resource Manager).
The middleware, based on the client's request, chooses a server to forward that request to. It then executes this request on that server's Resource Manager (thereby executing on that server via RMI).
In then returns the reply, which the client receives and handles.

The middleware handles customers. Adding and deleting customers is done within the middleware. Complex queries involving customers, e.g. flight reservations which demand knowledge of the flight as well as the customer, are split between the middleware and the servers. The middleware retrieves the information it needs from the servers, then uses this info to complete the request.

We chose to handle customers in this way because we felt it simplified the architecture of our program. The alternatives would be to have another server dedicated to customers, which would involve making a stripped down version of an RM and would leave more connections to handle. Or, we could have each server hold their own record of customers, and have an identical copy on each machine -- but this defeats the purpose of distributing the load and introduces unnecessary redundancy. A third approach would be to have a subset of the customer list on each of the servers, and aggreate the results when we need the full list of customers -- this, however, would really complicate the implementation as it involves much more sophisticated computation for every request involving customers.

## Example setup

```
cd comp512-project
```

On each server machine:

```
./serverscript <rmi-port>
```

On the middleware machine:

```
./middlewarescript <server1-hostname> <server1-port> <server2-hostname> <sever2-port> <server3-hostname> <server3-port> <middleware-rmi-port>
```

On the client machine:
```
./clientscript <middleware-hostname> <middleware-port> 
```

## Testing locally

To test everything locally, run

```
cd comp512-project
./testscript
```

This will launch three servers, a middleware, and two clients, all running on localhost.
