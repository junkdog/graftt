## Change Log (we do our best to adhere to [semantic versioning](http://semver.org/))

### 0.4.0-SNAPSHOT

### 0.3.0 - 2020-04-19

#### Breaking changes
- `graftt-maven-plugin`: `graftt` goal renamed to `transplant`

#### Simplified transplant disocovery
- `agent` now checks the classpath for any occurrences of `/graftt.index`: transplants
  listed in indices are registered by the agent automatically. The index files list one
  transplant per line using the qualified name of the transplant.
  - `graftt-maven-plugin:generate-index` to automatically generate it during build

- relocated internal dependencies of `agent` to allow for merging with external jars 
- **Fix**: publish [API docs] for `api` artifact
- **Fix**: Transplant substitution when fusing fields 

 [API docs]: https://www.javadoc.io/doc/net.onedaybeard.graftt/api/latest/index.html 


### 0.2.1 - 2020-01-01

- **Fix**: `Graft.Annotations::overwrite` does nothing
 

### 0.2.0 - 2019-12-30

#### Breaking changes
  - Java-agent key-value separator changed from `:` to `=` to work better
    under windows, e.g.: `-javaagent:${agent}=cp=${transplants.jar}`

- `@Graft.Fuse` annotations onto existing elements. Configure behavior
   with `@Graft.Annotations`. 

#### 0.1.3 - 2019-08-20

- Initial release
