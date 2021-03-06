/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.social.twitter;

import org.keycloak.social.AuthCallback;
import org.keycloak.social.AuthRequest;
import org.keycloak.social.SocialAccessDeniedException;
import org.keycloak.social.SocialProvider;
import org.keycloak.social.SocialProviderConfig;
import org.keycloak.social.SocialProviderException;
import org.keycloak.social.SocialUser;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.RequestToken;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TwitterProvider implements SocialProvider {

    @Override
    public String getId() {
        return "twitter";
    }

    @Override
    public AuthRequest getAuthUrl(SocialProviderConfig config) throws SocialProviderException {
        try {
            Twitter twitter = new TwitterFactory().getInstance();
            twitter.setOAuthConsumer(config.getKey(), config.getSecret());

            String redirectUri = config.getCallbackUrl();
            redirectUri = redirectUri.replace("//localhost", "//127.0.0.1");

            RequestToken requestToken = twitter.getOAuthRequestToken(redirectUri);

            return AuthRequest.create(requestToken.getToken(), requestToken.getAuthenticationURL())
                    .setAttribute("token", requestToken.getToken()).setAttribute("tokenSecret", requestToken.getTokenSecret())
                    .build();
        } catch (Exception e) {
            throw new SocialProviderException(e);
        }
    }

    @Override
    public String getName() {
        return "Twitter";
    }

    @Override
    public SocialUser processCallback(SocialProviderConfig config, AuthCallback callback) throws SocialProviderException {
        if (callback.getQueryParam("denied") != null) {
            throw new SocialAccessDeniedException();
        }

        try {
            Twitter twitter = new TwitterFactory().getInstance();
            twitter.setOAuthConsumer(config.getKey(), config.getSecret());

            String verifier = callback.getQueryParam("oauth_verifier");
            RequestToken requestToken = new RequestToken((String)callback.getAttribute("token"), (String)callback.getAttribute("tokenSecret"));

            twitter.getOAuthAccessToken(requestToken, verifier);
            twitter4j.User twitterUser = twitter.verifyCredentials();

            SocialUser user = new SocialUser(Long.toString(twitterUser.getId()), twitterUser.getScreenName());
            user.setName(twitterUser.getName());

            return user;
        } catch (Exception e) {
            throw new SocialProviderException(e);
        }
    }

}
