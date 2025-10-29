package com.darum.auth.domain.dto;

import java.util.Date;

public record AuthenticationResponse (String accessToken, Date expiresAt){}
