## Change Log (we do our best to adhere to [semantic versioning](http://semver.org/))

### 0.2.0-SNAPSHOT

- **Breaking change**
 - Java-agent key-value separator changed from `:` to `=` to work better
   under windows, e.g.: `-javaagent:${agent}=cp=${transplants.jar}`

- (WIP) `@Graft.Fuse` annotations onto existing elements. 
  Configure behavior with `@Graft.Annotations`. 

#### 0.1.3 - 2019-08-20

- Initial release
