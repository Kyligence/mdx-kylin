## Metadata Synchronization

- [Metadata synchronization principle](#Metadata synchronization principle)
- [Metadata synchronization types](#Metadata synchronization types)
- [Metadata synchronization frequency](#Metadata synchronization frequency)
- [Metadata failure retrial mechanism](#Metadata failure retrial mechanism)

### Metadata synchronization principle

Metadata synchronization is to get the relevant information from Kylin through HTTP call, and compare it with the existing metadata information in mdx. If there is any change, the metadata in mdx will be repaired according to the change of Kylin.

### Metadata synchronization types

Metadata synchronization information includes the following types:

1. User list synchronization

2. User group list synchronization

3. Cube list synchronization

4. Project list synchronization

5. Segment list synchronization

6. Cube metadata list synchronization

### Metadata synchronization frequency

For the above metadata types, the changes of items No.1 to No.4 are less difficult, more frequent and less expensive for users. They can be synchronized every 20 seconds and cannot be changed.

The synchronization frequency of item No.5 and No.6 are low. The default value is 300 seconds, that is, 5 minutes synchronization. The benchmark value is 20 seconds. The default configuration value is 15, that is 15 times of 20 seconds, which means 300 seconds. The configuration items are as follows:

insight.dataset.verify.interval.count=15

> Note ：
>
> When the synchronization task is massive, it may take longer than the configuration defined.

### Metadata failure retrial mechanism

Metadata synchronization task may stop because of user information change in Kylin. At this time, the metadata synchronization task will fail and try again with a frequency of 20 seconds. If the current synchronization user information or kylin returns to normal, the synchronization task returns to normal.
