package ro.garajulmeu.push;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Push configuration. Today it only declares which provider implementation is
 * active; the Firebase service credentials arrive with the real provider, which
 * cannot be built before the native applications of phases 17 and 18 exist.
 *
 * <p>Declaring the property here is not ceremony, and this class exists because
 * leaving it out was noticed by the IDE before it was noticed by anybody:
 * {@code spring-boot-configuration-processor} builds its metadata from
 * {@code @ConfigurationProperties} classes, so a key nothing declares is
 * literally an unknown property. The same warning was right about
 * {@code garajul-meu.ocr} on 2026-08-16 and was dismissed as a stale index.
 *
 * <p><strong>The default below does not select the provider.</strong>
 * {@code @ConditionalOnProperty} reads the raw environment and never sees a value
 * bound here, so an absent key still means no bean and a refusal to start - which
 * is the intended behaviour and the reason the seam is configured explicitly in
 * both application.yml files. This records the shape of the setting; it does not
 * supply it.
 *
 * @param provider {@code logging} writes notifications to the log, which is the
 *                 whole of V1 web; {@code firebase} delivers them for real
 */
@ConfigurationProperties(prefix = "garajul-meu.push")
public record PushProperties(@DefaultValue("logging") String provider) {
}