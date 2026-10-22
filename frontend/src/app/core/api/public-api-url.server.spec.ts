import { describe, expect, it } from 'vitest';
import { resolvePublicApiUrl } from './public-api-url.server';

describe('resolvePublicApiUrl', () => {
  it('falls back to the local API when the platform does not provide one', () => {
    expect(resolvePublicApiUrl('')).toBe('http://localhost:8080/api');
  });

  it('keeps an absolute URL that already carries the /api path', () => {
    expect(resolvePublicApiUrl('http://localhost:8080/api')).toBe('http://localhost:8080/api');
    expect(resolvePublicApiUrl('https://api.example.com/api/')).toBe('https://api.example.com/api');
  });

  it('appends /api when the platform hands only the origin', () => {
    expect(resolvePublicApiUrl('https://api.example.com')).toBe('https://api.example.com/api');
  });

  it('completes a bare hostname with https', () => {
    expect(resolvePublicApiUrl('api.example.com')).toBe('https://api.example.com/api');
  });
});
