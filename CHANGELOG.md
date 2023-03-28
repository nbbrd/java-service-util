# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- Force providers ordering in ClassPath to simplify debugging

## [1.6.0] - 2023-03-23

### Added

- Add property to disable warning on missing fallback [#208](https://github.com/nbbrd/java-service-util/issues/208)

### Fixed

- Fix batch reloading [#129](https://github.com/nbbrd/java-service-util/issues/129)

## [1.5.3] - 2023-03-21

### Changed

- Improve error messages on missing directives in module-info [#205](https://github.com/nbbrd/java-service-util/issues/205)
- Reduce verbosity of logging [#209](https://github.com/nbbrd/java-service-util/issues/209)

## [1.5.2] - 2022-10-28

### Fixed

- Fix dependency inheritance in BOM

## [1.5.1] - 2022-03-31

### Fixed

- Fix code generation on nested types

## [1.5.0] - 2022-03-30

### Added

- Add Maven BOM [#125](https://github.com/nbbrd/java-service-util/issues/125)
- Add batch loading of providers [#33](https://github.com/nbbrd/java-service-util/issues/33)

### Fixed

- Fix multi round processing of ServiceProvider

## [1.4.0] - 2021-05-20

### Added

- Add custom service loader [#74](https://github.com/nbbrd/java-service-util/issues/74)

### Fixed

- Fix possible collision in shadedPattern

## [1.3.2] - 2021-03-19

### Changed

- Migration to Maven-Central
- **Breaking change**: Maven groupId is now `com.github.nbbrd.java-service-util`

## [1.3.0] - 2020-03-30

### Added

- Added `@ServiceDefinition#wrapper` as basic preprocessing mapper

## [1.2.1] - 2019-10-18

### Fixed

- Fixed NPE in `ClassPathRegistry#getRelativeName(Name)` with some JDK

## [1.2.0] - 2019-10-16

### Added

- Added examples
- Added `load()` shortcut for local immutable instance
- Added `reset()` method alongside `reload()` method
- Added `ServiceFilter` and `ServiceSorter` annotations to simplify preprocessor
- Improved checks at compile time

## [1.1.0] - 2019-08-27

### Added

- Added `ServiceDefinition` annotation & processor to generate specialized service loader
- Added detection of implicit service type

### Changed

- Set processor as a shaded jar to minimize dependencies

### Fixed

- Fixed _"supported source version"_ warning
- Fixed detection of static `provider()` method

## [1.0.2] - 2019-08-13

### Fixed

- Fixed detection of services with generic parameters

## [1.0.1] - 2019-08-08

### Fixed

- Fixed providers binary names in classpath service files

## [1.0.0] - 2019-08-08

### Added

- Initial release

[Unreleased]: https://github.com/nbbrd/java-service-util/compare/v1.6.0...HEAD
[1.6.0]: https://github.com/nbbrd/java-service-util/compare/v1.5.3...v1.6.0
[1.5.3]: https://github.com/nbbrd/java-service-util/compare/v1.5.2...v1.5.3
[1.5.2]: https://github.com/nbbrd/java-service-util/compare/v1.5.1...v1.5.2
[1.5.1]: https://github.com/nbbrd/java-service-util/compare/v1.5.0...v1.5.1
[1.5.0]: https://github.com/nbbrd/java-service-util/compare/v1.4.0...v1.5.0
[1.4.0]: https://github.com/nbbrd/java-service-util/compare/v1.3.2...v1.4.0
[1.3.2]: https://github.com/nbbrd/java-service-util/compare/v1.3.0...v1.3.2
[1.3.0]: https://github.com/nbbrd/java-service-util/compare/v1.2.1...v1.3.0
[1.2.1]: https://github.com/nbbrd/java-service-util/compare/v1.2.0...v1.2.1
[1.2.0]: https://github.com/nbbrd/java-service-util/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/nbbrd/java-service-util/compare/v1.0.2...v1.1.0
[1.0.2]: https://github.com/nbbrd/java-service-util/compare/v1.0.1...v1.0.2
[1.0.1]: https://github.com/nbbrd/java-service-util/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/nbbrd/java-service-util/releases/tag/v1.0.0
