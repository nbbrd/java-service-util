package nbbrd.service.examples;

import nbbrd.service.Quantifier;
import nbbrd.service.ServiceDefinition;
import nbbrd.service.ServiceId;
import nbbrd.service.ServiceProvider;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Locale;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

@ServiceDefinition(quantifier = Quantifier.MULTIPLE, batchType = HashAlgorithm.Batch.class)
public interface HashAlgorithm {

    // 💡 Enforce service naming
    @ServiceId(pattern = ServiceId.SCREAMING_KEBAB_CASE)
    String getName();

    String hashToHex(byte[] input);

    static void main(String[] args) {
        // 💡 Retrieve service by name
        HashAlgorithmLoader.load()
                .stream()
                .filter(algo -> algo.getName().equals("SHA-256"))
                .findFirst()
                .map(algo -> algo.hashToHex("hello".getBytes(UTF_8)))
                .ifPresent(System.out::println);
    }

    interface Batch {
        Stream<HashAlgorithm> getProviders();
    }

    @ServiceProvider
    class MessageDigestBridge implements Batch {

        @Override
        public Stream<HashAlgorithm> getProviders() {
            return Security.getAlgorithms("MessageDigest")
                    .stream()
                    .map(MessageDigestAdapter::new);
        }
    }

    class MessageDigestAdapter implements HashAlgorithm {

        private final String name;

        public MessageDigestAdapter(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String hashToHex(byte[] input) {
            return bytesToHex(getThreadUnsafeInstance().digest(input));
        }

        private MessageDigest getThreadUnsafeInstance() {
            try {
                return MessageDigest.getInstance(name);
            } catch (NoSuchAlgorithmException ex) {
                throw new RuntimeException(ex);
            }
        }

        private static String bytesToHex(byte[] hash) {
            return String.format(Locale.ROOT, "%0" + (hash.length << 1) + "x", new BigInteger(1, hash));
        }
    }
}
