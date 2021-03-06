# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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

[Unreleased]: https://github.com/nbbrd/java-service-util/compare/v1.4.0...HEAD
[1.4.0]: https://github.com/nbbrd/java-service-util/compare/v1.3.2...v1.4.0
[1.3.2]: https://github.com/nbbrd/java-service-util/compare/v1.3.0...v1.3.2
[1.3.0]: https://github.com/nbbrd/java-service-util/compare/v1.2.1...v1.3.0
[1.2.1]: https://github.com/nbbrd/java-service-util/compare/v1.2.0...v1.2.1
[1.2.0]: https://github.com/nbbrd/java-service-util/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/nbbrd/java-service-util/compare/v1.0.2...v1.1.0
[1.0.2]: https://github.com/nbbrd/java-service-util/compare/v1.0.1...v1.0.2
[1.0.1]: https://github.com/nbbrd/java-service-util/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/nbbrd/java-service-util/releases/tag/v1.0.0
