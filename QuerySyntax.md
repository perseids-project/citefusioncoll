# CITE Collection Query Extension #

Initial rough notes:

## `list` servlet ##

- must have a `coll` parameter giving the URN of a Collection (e.g., `urn:cite:op:greek`)

If no other parameters are given, this lists all objects in the Collection.

Optionally, may specify a set of one or more query triplets comprised of a property name, a value, and an operator.

- one or more `prop` parameters. These give names of properties to query on.  If you supply more than one `prop` parameter, the criteria will be `AND`ed together.
- for each `prop` parameter, you must include one parameter giving the value to query for.  The name of this parameter must match the value of one of the  `prop` parameters.  E.g., if you have `prop=Description` then you must include a parameter named `Description` giving the value to query for.
- The default operator is "=".  If you want to specify a different operator, append it to the property value separated by a colon.  E.g., to ask for objects with a Description property beginning with the string `alph`, you could use `prop=Description&Description=alph:+STARTS+WITH+`


### Some `list` query examples ###

Find all OP observations described as 'alpha':  

    list?coll=urn:cite:op:greek&prop=Description&Description=alpha

Find OP observations described as 'alpha' and visual class 'uncial'

    list?coll=urn:cite:op:greek&prop=Description&Description=alpha&prop=VisualClass&VisualClass=uncial


Find OP observations with description beginning with the string 'alpha'

    list?coll=urn:cite:op:greek&prop=Description&Description=alph:+STARTS+WITH+


## `imgmap` ##

In a collection with an image property, you can use this servlet to get a list of values to map onto image RoIs.

Required parameters:

- a URN identifying a collection  (e.g., `urn:cite:op:greek`)
- a URN for an image
- the name of a property to map onto the image
- the name of the property with the image values to use





