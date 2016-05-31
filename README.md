![Imgur](http://i.imgur.com/KHciDE9.png)

# rx-awesome
A demonstration of different Rx techniques to solve common problems.

## RxDemos

### Share should prevent projection work from being repeated

Normally, when you take a stream and project on it (like `stream.map(_ * 2)`), the work is repeated for each subscriber.
If you prefer the work not be repeated, you can use `.share`. This test demonstrates share by counting the number of 
times a mapper function is called.

### Cold Observables should repeat projection work if not shared

When you take a stream and project on it (like `stream.map(_ * 2)`), the work is repeated for each subscriber. 
This test demonstrates that by counting the number of times a mapper function is called.

### Cold Observable should emit the same result to each subscriber

[Cold observables](http://reactivex.io/documentation/observable.html), whose contents are pre-defined before the observable 
is created, replays all the data to each subscriber.

#### Grouping into a readable state should go like this

Often times in your application, you are compelled to maintain state. This test demonstrates how to compose streams to 
produce a state stream that -- in addition to emitting state changes in real-time -- emit the latest state to new 
subscribers.

#### Endless observables should never complete

When a stream never emits an OnCompleted event, some operators like .sum don't function. This test demonstrates that.

#### User interactions should be considered a stream

In an event driven system, what would the login button of a login UI do? Call a REST api? Update the UI? Well, no. 
A UI element's job should be to post an event. This test demonstrates how to achieve rich UI behavior by composing streams.

