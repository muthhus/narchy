Since when the paper "Natural Language Processing by Reasoning and Learning" bei Pei was written, NAL7 was not yet aviable,
I decided to make use of the ideas in this paper again, especially to show how relational and temporal reasoning can form a whole,
and also because my other natural language experiments were mainly only capturing the temporal aspect while Pei's examples where capturing only the relational aspect of natural language learning.
There are already some people who want to use NARS for natural language, and I think the next 4 examples nicely show how NARS stretches the boundaries of natural language understanding of computers.

The examples show:
- Example1: A simple example of giving the system evidence for how a bigger structure can probably be understood by understanding its parts.
- Example2: A example of giving the system evidence for creating own REPRESENT relation statements by observations of events, consistent of how it also approximately happens in human language learning (providing a spoken or written word/sentence with as much relevant context as possible and let the system create the relevant associations by its own)
- Example3: Understanding the meaning of a new sentence by using a result from Example2 and a input statement of Example1.
- Example4: (using the conclusion of Example3) Temporal reasoning involving an event which was purely the result of a own interpretation of the sentence and not directly observable at all, leading to the hypothesis "if I hear a sentence which represents that tim is eating food (without mentioning the specific used words at all or what he eats) then I may smell food". <- This is the sort of transcending of particular observations I always wanted to see and I claim that most classical machine learning techniques are unable to do this fundamentally.
- Example5: Using the learned consequence of the by itself not directly observable interpretation event (&&, <#1 --> (/, REPRESENT, _, (*,<{TIM} --> [EATING]>, FOOD))>, <#1 --> WORDS>) to predict an observable event.

All 5 examples work with OpenNARS 1.7.0, and only example 2 works with 1.6.4 interestingly, have fun:

----
```
////Example 1, REPRESENT relation with lifting
//the whole can sometimes be understood by understanding what the parts represent (lifting)
<(&&,<$1 --> (/,REPRESENT,_,$3)>,<$2 --> (/,REPRESENT,_,$4)>) ==> <(*,(*,$1,$2),(*,$3,$4)) --> REPRESENT>>.
//the word fish represents the concept FOOD
<cat --> (/,REPRESENT,_,ANIMAL)>.
//the word eats represents the concept EATING
<eats --> (/,REPRESENT,_,EATING)>.

//what does cat eats represent?
<(*,(*,cat,eats),?what) --> REPRESENT>?
//RESULT: <(*,(*,cat,eats),(*,ANIMAL,EATING)) --> REPRESENT>. %1.00;0.73%
```
---

````
////Example 2, giving it primitive evidence for the meaning of our custom REPRESENT relation
////in such a way that it uses our REPRESENT relation for describing concurrent observations of words and a event
//if the words is used concurrently with the situation, the word may represent what was observed
<(&|,<$1 --> WORDS>,<$2 --> $3>) ==> <(*,$1,<$2 --> $3>) --> REPRESENT>>.
//the words "tim eats" are observed
<(*,tim,eats) --> WORDS>. :|:
//it is observed that tim is eating
<{TIM} --> [EATING]>. :|:

//what do the words "tim eats" represent?
<(*,(*,tim,eats),?what) --> REPRESENT>?
//RESULT: <(*,(*,tim, eats), <{TIM} --> [EATING]>) --> REPRESENT>. %1.00;0.29%
````

----

````
////Example3: showing the potential of both together
//using the result from the last example:
<(*,(*,tim, eats), <{TIM} --> [EATING]>) --> REPRESENT>. %1.00;0.29%
//the word fish represents FOOD
<fish --> (/,REPRESENT,_,FOOD)>.
//the whole can sometimes be understood by understanding what the parts represent (lifting)
<(&&,<$1 --> (/,REPRESENT,_,$3)>,<$2 --> (/,REPRESENT,_,$4)>) ==> <(*,(*,$1,$2),(*,$3,$4)) --> REPRESENT>>.

//what does tim eat fish represent?
<(*,(*,(*,tim,eats),fish),?what) --> REPRESENT>?
//RESULT: <(*,(*,(*,tim,eats),fish),(*,<{TIM} --> [EATING]>,FOOD)) --> REPRESENT>. %1.00;0.23%
````

----

````
////Example4: Temporal reasoning on not directly observable events coming from interpretations of words
//tim eats fish are the observed words
<(*,(*,tim,eats),fish) --> WORDS>. :|:
//lets say system would already know the interpretation Example3 resulted in
<(*,(*,(*,tim,eats),fish),(*,<{TIM} --> [EATING]>,FOOD)) --> REPRESENT>.
//I smell some food
<(*,SELF,FOOD) --> smell>. :|:

//does someone saying something which represents that tim is eating food imply that I smell food?
<(&&, <#1 --> (/, REPRESENT, _, (*,<{TIM} --> [EATING]>, FOOD))>, <#1 --> WORDS>) =|> <(*,SELF,FOOD) --> smell>>?
//RESULT: <(&&, <#1 --> (/, REPRESENT, _, (*,<{TIM} --> [EATING]>, FOOD))>, <#1 --> WORDS>) =|> <(*,SELF,FOOD) --> smell>>. %1.00;0.18%

````

----

````

////Example5: Making use of the transcended knowledge which was obtained by dependent variable introduction and induction in Example4
//if someone says something which represents that tim is eating food then it might be that I smell food
<(&&, <#1 --> (/, REPRESENT, _, (*,<{TIM} --> [EATING]>, FOOD))>, <#1 --> WORDS>) =|> <(*,SELF,FOOD) --> smell>>. %1.00;0.18%
//lets say system would already know the interpretation Example3 resulted in
<(*,(*,(*,tim,eats),fish),(*,<{TIM} --> [EATING]>,FOOD)) --> REPRESENT>.
//the sentence "tim eats fish" is observed
<(*,(*,tim,eats),fish) --> WORDS>. :|:

//do I smell food?
<(*,SELF,FOOD) --> smell>?
//RESULT: <(*,SELF,FOOD) --> smell>. %1.00;0.13%
````

----

Best regards,
Patrick