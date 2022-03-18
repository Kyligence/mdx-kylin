/*
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/
import { connect } from 'react-redux';
import mapReselectToProps from './reselect';
import mapDispatchToProps from './actions';

function isUndefined(value) {
  return value === undefined;
}

function withDefaultValue(stateValue, defaultValue) {
  return isUndefined(stateValue) ? defaultValue : stateValue;
}

function getMapStoreToProps(namespace = '', mapState = {}, mapReselect = {}, defaultState = {}) {
  return function mapStoreToProps(state) {
    const stateToProps = {};
    const reselectToProps = {};
    const allReselect = mapReselectToProps(state);
    const namespaceArray = namespace.split('/').filter(item => !!item);

    for (const stateKey of Object.keys(mapState)) {
      const namespacedState = namespaceArray.reduce(
        (currentState, namespaceKey) => currentState[namespaceKey], state,
      ) || {};

      const defaultValue = defaultState[stateKey];
      stateToProps[stateKey] = withDefaultValue(
        mapState[stateKey](namespacedState), defaultValue,
      );
    }

    for (const selectKey of Object.keys(mapReselect)) {
      const defaultValue = defaultState[selectKey];
      reselectToProps[selectKey] = withDefaultValue(
        mapReselect[selectKey](allReselect), defaultValue,
      );
    }

    return { ...stateToProps, ...reselectToProps };
  };
}

export function Connect(...args) {
  return component => {
    const mappers = args.map(params => {
      const { namespace, mapState, mapReselect, defaultState } = params;
      return getMapStoreToProps(namespace, mapState, mapReselect, defaultState);
    });

    const options = args.reduce((allOptions, params) => (
      { ...allOptions, ...(params.options || {}) }
    ), {});

    const mapStateToProps = store => mappers.reduce((states, mapper) => (
      { ...states, ...mapper(store) }
    ), {});

    const reduxComponent = connect(mapStateToProps, mapDispatchToProps, null, options)(component);
    reduxComponent.jest = connect(mapStateToProps, null, null, options)(component);
    return reduxComponent;
  };
}
