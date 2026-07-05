import { useCallback, useEffect, useState } from 'react';
import { routeByKey, routeKeyFromPath } from '../lib/routes';

export function useAppRoute() {
  const [activeKey, setActiveKey] = useState(() => routeKeyFromPath(window.location.pathname));

  useEffect(() => {
    const onPopState = () => setActiveKey(routeKeyFromPath(window.location.pathname));
    window.addEventListener('popstate', onPopState);
    return () => window.removeEventListener('popstate', onPopState);
  }, []);

  const navigate = useCallback((key: string) => {
    const route = routeByKey(key);
    if (window.location.pathname !== route.path) {
      window.history.pushState({}, '', route.path);
    }
    setActiveKey(route.key);
  }, []);

  return { activeKey, navigate };
}
